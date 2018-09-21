package org.openmrs.module.appointments.web.controller;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearch;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.web.contract.AppointmentDefaultResponse;
import org.openmrs.module.appointments.web.mapper.AppointmentMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentsControllerTest {

    @Mock
    private AppointmentsService appointmentsService;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentsController appointmentsController;

    @Test
    public void shouldGetAppointmentByUuid() throws Exception {
        String appointmentUuid = "random";
        Appointment appointment = new Appointment();
        appointment.setUuid(appointmentUuid);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(appointment);
        appointmentsController.getAppointmentByUuid(appointmentUuid);
        verify(appointmentsService, times(1)).getAppointmentByUuid(appointmentUuid);
        verify(appointmentMapper, times(1)).constructResponse(appointment);
    }

    @Test
    public void shouldGetAppointmentsBetweenGivenDateRange() {
        AppointmentSearch appointmentSearch = new AppointmentSearch();
        ArrayList<Appointment> appointments = new ArrayList<>();
        ArrayList<AppointmentDefaultResponse> expectedResponse = new ArrayList<>();
        when(appointmentsService.search(appointmentSearch)).thenReturn(appointments);
        when(appointmentMapper.constructResponse(appointments)).thenReturn(expectedResponse);

        List<AppointmentDefaultResponse> actualResponse = appointmentsController.search(appointmentSearch);

        verify(appointmentsService, times(1)).search(appointmentSearch);
        verify(appointmentMapper, times(1)).constructResponse(appointments);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenAppointmentsServiceSearchMethodReturnsNull() {
        AppointmentSearch appointmentSearch = new AppointmentSearch();
        when(appointmentsService.search(appointmentSearch)).thenReturn(null);

        appointmentsController.search(appointmentSearch);

        verify(appointmentsService, times(1)).search(appointmentSearch);
        verify(appointmentMapper, never()).constructResponse(anyListOf(Appointment.class));
    }

    @Test
    public void shouldChangeStatusOfAppointment() throws Exception {
        Map statusDetails = new HashMap();
        statusDetails.put("toStatus", "Completed");
        Appointment appointment = new Appointment();
        when(appointmentsService.getAppointmentByUuid(anyString())).thenReturn(appointment);

        appointmentsController.transitionAppointment("appointmentUuid", statusDetails);

        verify(appointmentsService, times(1)).getAppointmentByUuid("appointmentUuid");
        verify(appointmentsService, times(1)).changeStatus(appointment, "Completed", null);
    }

    @Test
    public void shouldReturnErrorResponseWhenAppointmentDoesNotExist() throws Exception {
        Map<String, String> statusDetails = new HashMap();
        statusDetails.put("toStatus", "Completed");
        when(appointmentsService.getAppointmentByUuid(anyString())).thenReturn(null);

        appointmentsController.transitionAppointment("appointmentUuid", statusDetails);

        verify(appointmentsService, times(1)).getAppointmentByUuid("appointmentUuid");
        verify(appointmentsService, never()).changeStatus(any(), any(), any());
    }
}
