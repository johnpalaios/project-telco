package eu.advantage.fibernow.service;

import eu.advantage.fibernow.converter.DomainToDtoConverter;
import eu.advantage.fibernow.dto.CustomerDto;
import eu.advantage.fibernow.exception.BusinessException;
import eu.advantage.fibernow.model.Admin;
import eu.advantage.fibernow.model.Customer;
import eu.advantage.fibernow.model.Ticket;
import eu.advantage.fibernow.model.enums.UserStatus;
import eu.advantage.fibernow.model.enums.TicketStatus;
import eu.advantage.fibernow.repository.CustomerRepository;
import eu.advantage.fibernow.repository.GenericRepository;
import eu.advantage.fibernow.repository.TicketRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.advantage.fibernow.converter.DomainToDtoConverter.toDto;
import static eu.advantage.fibernow.converter.DtoToDomainConverter.*;
import static eu.advantage.fibernow.exception.ExceptionStatus.*;

@Stateless
@Slf4j
public class CustomerServiceImpl extends AbstractUserService<Customer> implements CustomerService {

    @Inject
    private CustomerRepository customerRepository;

    @Inject
    private TicketRepository ticketRepository;
    @Inject
    private UserService<Admin> adminUserService;
    @Inject
    private UserService<Customer> customerUserService;

    @Override
    @Transactional
    public CustomerDto saveCustomer(CustomerDto dto) throws BusinessException{
        log.info("Called saveCustomer() with CustomerDto = {}", dto);
        Customer customer = toDomain(dto);
        if(checkIfUsernameExists(customer.getCredentials().getUsername(), customer.getId())) {
            throw new BusinessException(BZ_ERROR_1010, customer.getCredentials().getUsername());
        }
        if (customer.getTickets() == null) {
            customer.setTickets(new HashSet<>());
        }
        customer.getTickets().forEach(ticket -> ticket.setCustomer(customer));
        if (customer.getUserStatus() == null) {
            customer.setUserStatus(UserStatus.ACTIVE);
        }
        List<CustomerDto> tinCustomers = customerRepository
                .searchByCriteria("tin", customer.getTin()).stream()
                .map(DomainToDtoConverter::toDto)
                .collect(Collectors.toList());
        List<CustomerDto> emailCustomers = customerRepository
                .searchByCriteria("email", customer.getEmail()).stream()
                .map(DomainToDtoConverter::toDto)
                .collect(Collectors.toList());
        if (customer.getId() == null) {

            if (!tinCustomers.isEmpty()) {
                throw new BusinessException(BZ_ERROR_1003, customer.getTin());
            }
            if (!emailCustomers.isEmpty()) {
                throw new BusinessException(BZ_ERROR_1004, customer.getEmail());
            }
            customerRepository.create(customer);
            log.info("Customer {} - Created.", customer);
        }
        else {
            Customer found = customerRepository.findById(customer.getId());
            if (found == null) {
                log.error("Customer {} that is trying to be updated doesn't exist.", customer);
                throw new BusinessException(BZ_ERROR_1001, customer.getId());
            }
            if (!tinCustomers.isEmpty() && !Objects.equals(tinCustomers.get(0).getId(), customer.getId())) {
                log.error("Customer {} that is trying to be updated has the same tin as another customer = {}", customer, tinCustomers.get(0));
                throw new BusinessException(BZ_ERROR_1005, customer.getTin());
            }
            if (!emailCustomers.isEmpty() && !Objects.equals(emailCustomers.get(0).getId(), customer.getId())) {
                log.error("Customer {} that is trying to be updated has the same email as another customer = {}", customer, tinCustomers.get(0));
                throw new BusinessException(BZ_ERROR_1006, customer.getEmail());
            }
            customerRepository.update(customer);
            log.info("Customer {} - Updated.", customer);
        }
        return toDto(customer);
    }

    @Override
    public CustomerDto findCustomer(Long id) throws BusinessException{
        log.info("Called findCustomer() with id = {}.", id);
        Customer found = customerRepository.findById(id);
        if (found == null) {
            log.error("Customer with Id : {} wasn't found.", id);
            throw new BusinessException(BZ_ERROR_1001, id);
        }
        Set<Ticket> tickets = new HashSet<>(ticketRepository.findTicketsByCustomer(found));
        found.setTickets(tickets);
        CustomerDto foundDto = toDto(found);
        log.info("Customer with Id {} was found = {}" , id, foundDto);
        return foundDto;
    }

    @Override
    public List<CustomerDto> searchCustomers(String email, String tin) throws BusinessException{
        log.info("Called searchCustomers() with email = {}, tin = {}.", email, tin);
        if(tin != null) {
            log.info("Going to search for Customer only with tin = {}.", tin);
            List<CustomerDto> found = customerRepository
                    .searchByCriteria("tin", tin).stream()
                    .map(DomainToDtoConverter::toDto)
                    .collect(Collectors.toList());
            if (found.isEmpty()) {
                log.error("No customers found with this tin = {}", tin);
                throw new BusinessException(BZ_ERROR_1008, tin);
            }
            log.info("Customers found with tin={} : {}", tin, found);
            return found;
        }
        if(email != null) {
            log.info("Going to search for Customer only with email = {}. (tin = {})", email, tin);
            List<CustomerDto> found = customerRepository
                    .searchByCriteria("email", email).stream()
                    .map(DomainToDtoConverter::toDto)
                    .collect(Collectors.toList());

            if (found.isEmpty()) {
                log.error("No customers found with this email = {}", email);
                throw new BusinessException(BZ_ERROR_1009, email);
            }
            log.info("Customers found with email={} : {}", email, found);
            return found;
        }
        log.info("Both [email={}] and [tin={}] are null so all the customers are returned.", email, tin);
        List<CustomerDto> found =  customerRepository
                .findAll()
                .stream()
                .map(DomainToDtoConverter::toDto)
                .collect(Collectors.toList());

        if (found.isEmpty()) {
            log.error("No customers found in the DB");
            throw new BusinessException(BZ_ERROR_1002);
        }
        return found;
    }

    @Override
    @Transactional
    public CustomerDto deleteCustomer(Long id) throws BusinessException{
        log.info("Called deleteCustomer() with id = {}.", id);
        Customer found = customerRepository.findById(id);
        if (found == null) {
            log.error("Trying to delete a Customer with Id = {} that doesn't exist", id);
            throw new BusinessException(BZ_ERROR_1001, id);
        }

        Set<Ticket> tickets = found.getTickets();
        if (tickets != null) {
            tickets.forEach(ticket -> {
                TicketStatus status = ticket.getTicketStatus();
                if ( status != TicketStatus.DELETED && status != TicketStatus.COMPLETED) {
                    log.error("Trying to delete a Customer with Id = {} that has Ticket = {} not Deleted or Completed", id, ticket);
                    throw new BusinessException(BZ_ERROR_1007, id);
                }
            });
        }
        if(found.getUserStatus() == UserStatus.DELETED) {
            throw new BusinessException(BZ_ERROR_1011,id);
        }
        //Soft Delete
        found.setUserStatus(UserStatus.DELETED);
        customerRepository.update(found);
        log.info("Deleted : Customer {}", found);
        return toDto(found);
    }

    @Override
    public CustomerDto findCustomerByUsername(String username) {
        Customer customer = customerUserService.getUserByUsername(username);
        if(customer == null) {
            throw new BusinessException(BZ_ERROR_1012,username);
        }
        return toDto(customer);
    }

    @Override
    public GenericRepository<Customer, Long> getRepository() {
        return customerRepository;
    }

    private boolean checkIfUsernameExists(String username, Long id) {
        if(adminUserService.getUserByUsername(username) != null) return true;
        Customer customer = customerUserService.getUserByUsername(username);
        if(customer == null) return false;
        log.info("This is customer.getId() : {} and parameter id : {}", customer.getId(), id);
        return !(customer.getId() == id);
    }
}
