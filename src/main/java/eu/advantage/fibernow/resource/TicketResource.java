package eu.advantage.fibernow.resource;

import eu.advantage.fibernow.dto.TicketDto;
import eu.advantage.fibernow.service.TicketService;

import eu.advantage.fibernow.util.rest.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class TicketResource {
        @Inject
        private TicketService service;

        @GET
        @Path("/{id}")
        @RolesAllowed("ADMIN")
        public Response getById(@PathParam("id") Long id) {
            return Response
                    .status(Response.Status.ACCEPTED)
                    .entity(ApiResponse.builder().data(service.findTicket(id)).build())
                    .build();
        }

        @GET
        @Path("/top10")
        @RolesAllowed("ADMIN")
        public Response getTopTen(@QueryParam("datetime") String dateTimeString) {
            LocalDateTime dateTime;
            if(dateTimeString != null)  {
                dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                dateTime = null;
            }
            return Response
                    .status(Response.Status.ACCEPTED)
                    .entity(ApiResponse.builder().data(service.findTop10TicketsAfterDate(dateTime)).build())
                    .build();
        }
        @GET
        @RolesAllowed("ADMIN")
        public Response search(@QueryParam("customerId") Long customerId, @QueryParam("startDate") String startDateString,
                               @QueryParam("endDate") String endDateString) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = null;
            LocalDate endDate = null;
            if(startDateString != null) {
                startDate = LocalDate.parse(startDateString, formatter);
            }
            if(endDateString != null) {
                endDate = LocalDate.parse(endDateString, formatter);
            }
            List<TicketDto> result = service.searchTickets(customerId, startDate, endDate);
            return Response
                    .status(Response.Status.ACCEPTED)
                    .entity(ApiResponse.builder().data(result).build())
                    .build();
        }

        @POST
        @RolesAllowed("ADMIN")
        public Response save(@Valid TicketDto ticketDto) {
            TicketDto result = service.saveTicket(ticketDto);
            return Response.created(UriBuilder
                            .fromResource(eu.advantage.fibernow.resource.TicketResource.class)
                            .path("/" + result.getId())
                            .build()
                    )
                    .entity(ApiResponse.builder().data(result).build())
                    .build();
        }

        @DELETE
        @Path("/{id}")
        @RolesAllowed("ADMIN")
        public Response delete(@PathParam("id") Long id) {

            return Response
                    .status(Response.Status.ACCEPTED)
                    .entity(ApiResponse.builder().data(service.deleteTicket(id)).build())
                    .build();
        }

}
