package org.openmrs.module.appointments.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Patient;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentService;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class AppointmentsServiceTest extends BaseModuleWebContextSensitiveTest {
    private String adminUser;
    private String adminUserPassword;
    private String manageUser;
    private String manageUserPassword;
    private String manageSelfUser;
    private String manageSelfUserPassword;
    private String readOnlyUser;
    private String readOnlyUserPassword;
    private String noPrivilegeUser;
    private String noPrivilegeUserPassword;

    @Autowired
    AppointmentsService appointmentsService;

    @Before
    public void init() throws Exception {
        adminUser = "super-user";
        adminUserPassword = "P@ssw0rd";
        manageUser = "manage-user";
        manageUserPassword = "P@ssw0rd";
        manageSelfUser = "manage-self-user";
        manageSelfUserPassword = "test";
        readOnlyUser = "read-only-user";
        readOnlyUserPassword = "P@ssw0rd";
        noPrivilegeUser = "no-privilege-user";
        noPrivilegeUserPassword = "P@ssw0rd";
        executeDataSet("userRolesandPrivileges.xml");
    }

    @Test
    public void shouldSaveAppointmentsOnlyIfUserHasManagePrivilege() throws Exception {
        Context.authenticate(manageUser, manageUserPassword);
        Appointment appointment = getSampleAppointment();
        assertNotNull(appointmentsService.validateAndSave(appointment));
    }

    @Test
    public void shouldSaveAppointmentsOnlyIfUserHasManageSelfPrivilege() throws Exception {
        Context.authenticate(manageSelfUser, manageSelfUserPassword);
        Appointment appointment = getSampleAppointment();
        assertNotNull(appointmentsService.validateAndSave(appointment));
    }

    private Appointment getSampleAppointment() throws ParseException {
        Appointment appointment = new Appointment();
        appointment.setPatient(new Patient());
        appointment.setService(new AppointmentService());
        Date startDateTime = DateUtil.convertToDate("2108-08-15T10:00:00.0Z", DateUtil.DateFormatType.UTC);
        Date endDateTime = DateUtil.convertToDate("2108-08-15T10:30:00.0Z", DateUtil.DateFormatType.UTC);
        appointment.setStartDateTime(startDateTime);
        appointment.setEndDateTime(endDateTime);
        appointment.setAppointmentKind(AppointmentKind.Scheduled);
        return appointment;
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotSaveAppointmentsIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.validateAndSave(new Appointment()));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotSaveAppointmentIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.validateAndSave(new Appointment()));
    }

    @Test
    public void shouldGetAllAppointmentsIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointments(null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllAppointmentsIfUserDoesNotHaveAnyPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.getAllAppointments(null));
    }

    @Test
    public void shouldBeAbleToSearchAppointmentsIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.search(new Appointment()));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToSearchAppointmentsIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertNotNull(appointmentsService.search(new Appointment()));
    }

    @Test
    public void shouldGetAllFutureAppointmentsIfUserHasReadOnlyPrivilege() {
        Context.authenticate(manageUser, manageUserPassword);
        AppointmentService appointmentService = new AppointmentService();
        appointmentService.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForService(appointmentService));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllFutureAppointmentsForServiceIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentService appointmentService = new AppointmentService();
        appointmentService.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForService(appointmentService));
    }

    @Test
    public void shouldGetAllFutureAppointmentsForServiceTypeIfUserHasReadOnlyPrivilege() {
        Context.authenticate(adminUser, adminUserPassword);
        AppointmentServiceType appointmentServiceType = new AppointmentServiceType();
        appointmentServiceType.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllFutureAppointmentsForServiceTypeIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentServiceType appointmentServiceType = new AppointmentServiceType();
        appointmentServiceType.setId(1);
        assertNotNull(appointmentsService.getAllFutureAppointmentsForServiceType(appointmentServiceType));
    }

    @Test
    public void shouldGetAppointmentsForServiceIfUserHasReadOnlyPrivilege() {
        Context.authenticate(manageUser, manageUserPassword);
        AppointmentService appointmentService = new AppointmentService();
        appointmentService.setId(1);
        assertNotNull(appointmentsService.getAppointmentsForService(appointmentService, null, null, null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAppointmentsForServiceIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        AppointmentService appointmentService = new AppointmentService();
        appointmentService.setId(1);
        assertNotNull(appointmentsService.getAppointmentsForService(appointmentService, null, null, null));
    }

    @Test
    public void shouldGetAppointmentByUuidIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertEquals(null, appointmentsService.getAppointmentByUuid("uuid"));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAppointmentByUuidIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, noPrivilegeUserPassword);
        assertEquals(null, appointmentsService.getAppointmentByUuid("uuid"));
    }

    @Test
    public void shouldBeAbleToChangeStatusIfUserHasManagePrivilege() {
        Context.authenticate(manageUser, manageUserPassword);
        appointmentsService.changeStatus(new Appointment(), "Completed", null);
    }

    @Test
    public void shouldBeAbleToChangeStatusIfUserHasManageSelfPrivilege() {
        Context.authenticate(manageSelfUser, manageSelfUserPassword);
        appointmentsService.changeStatus(new Appointment(), "Completed", null);
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToChangeStatusIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        appointmentsService.changeStatus(new Appointment(), "Completed", null);
    }

    @Test
    public void shouldGetAllAppointmentsInDateRangeIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointmentsInDateRange(null, null));
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotGetAllAppointmentsInDateRangeIfUserHasNoPrivilege() {
        Context.authenticate(noPrivilegeUser, readOnlyUserPassword);
        assertNotNull(appointmentsService.getAllAppointmentsInDateRange(null, null));
    }

    @Test(expected = APIException.class)
    public void shouldBeAbleToUndoStatusChangeIfUserHasManagePrivilege() {
        Context.authenticate(manageUser, manageUserPassword);
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointmentsService.undoStatusChange(appointment);
    }

    @Test(expected = APIException.class)
    public void shouldBeAbleToUndoStatusChangeIfUserHasManageSelfPrivilege() {
        Context.authenticate(manageSelfUser, manageSelfUserPassword);
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointmentsService.undoStatusChange(appointment);
    }

    @Test(expected = APIAuthenticationException.class)
    public void shouldNotBeAbleToUndoStatusChangeIfUserHasReadOnlyPrivilege() {
        Context.authenticate(readOnlyUser, manageUserPassword);
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointmentsService.undoStatusChange(appointment);
    }
}
