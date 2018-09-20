package org.openmrs.module.appointments.web.controller;

import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearch;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.module.appointments.web.contract.AppointmentDefaultResponse;
import org.openmrs.module.appointments.web.mapper.AppointmentMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/appointments")
public class AppointmentsController {
    @Autowired
    private AppointmentsService appointmentsService;
    @Autowired
    private AppointmentMapper appointmentMapper;

    @RequestMapping(method = RequestMethod.GET, value="/{uuid}")
    @ResponseBody
    public AppointmentDefaultResponse getAppointmentByUuid(@PathVariable(value = "uuid") String uuid)  {
        Appointment appointment = appointmentsService.getAppointmentByUuid(uuid);
        if (appointment == null) {
            throw new RuntimeException("Appointment does not exist");
        }
        return appointmentMapper.constructResponse(appointment);
    }

    @RequestMapping(method = RequestMethod.POST, value = "search")
    @ResponseBody
    public List<AppointmentDefaultResponse> search(@Valid @RequestBody AppointmentSearch appointmentSearch) {
        List<Appointment> appointments = appointmentsService.search(appointmentSearch);
        if(isNull(appointments)){
            throw new RuntimeException("Either StartDate or EndDate not provided");
        }
        return appointmentMapper.constructResponse(appointments);
    }
}
