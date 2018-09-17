package org.openmrs.module.appointments.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.dao.AppointmentAuditDao;
import org.openmrs.module.appointments.dao.AppointmentDao;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentAudit;
import org.openmrs.module.appointments.model.AppointmentSearch;
import org.openmrs.module.appointments.model.AppointmentService;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.validator.AppointmentStatusChangeValidator;
import org.openmrs.module.appointments.validator.AppointmentValidator;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Transactional
public class AppointmentsServiceImpl implements AppointmentsService {

    private static final String MANAGE_APPOINTMENTS_PRIVILEGE = "Manage Appointments";
    private static final String RESET_APPOINTMENT_STATUS_PRIVILEGE = "Reset Appointment Status";
    private static final String PRIVILEGES_EXCEPTION_CODE = "error.privilegesRequired";

    AppointmentDao appointmentDao;

    List<AppointmentStatusChangeValidator> statusChangeValidators;

    List<AppointmentValidator> appointmentValidators;

    AppointmentAuditDao appointmentAuditDao;

    public void setAppointmentDao(AppointmentDao appointmentDao) {
        this.appointmentDao = appointmentDao;
    }

    public void setStatusChangeValidators(List<AppointmentStatusChangeValidator> statusChangeValidators) {
        this.statusChangeValidators = statusChangeValidators;
    }

    public void setAppointmentValidators(List<AppointmentValidator> appointmentValidators) {
        this.appointmentValidators = appointmentValidators;
    }

    public void setAppointmentAuditDao(AppointmentAuditDao appointmentAuditDao) {
        this.appointmentAuditDao = appointmentAuditDao;
    }

    private boolean validateUserAccess(Appointment appointment) {
        if (Context.hasPrivilege(MANAGE_APPOINTMENTS_PRIVILEGE)) {
            return true;
        }
        if (appointment.getProvider() != null && appointment.getProvider().getPerson() != null &&
                !appointment.getProvider().getPerson().equals(Context.getAuthenticatedUser().getPerson())) {
            return false;
        }
        return true;
    }

    @Override
    public Appointment validateAndSave(Appointment appointment) throws APIException {
        if (!validateUserAccess(appointment)) {
            throw new APIAuthenticationException(Context.getMessageSourceService().getMessage(PRIVILEGES_EXCEPTION_CODE,
                    new Object[]{MANAGE_APPOINTMENTS_PRIVILEGE}, null));
        }
        if (!CollectionUtils.isEmpty(appointmentValidators)) {
            List<String> errors = new ArrayList<>();
            for (AppointmentValidator validator : appointmentValidators) {
                validator.validate(appointment, errors);
            }
            if (!errors.isEmpty()) {
                String message = StringUtils.join(errors, "\n");
                throw new APIException(message);
            }
        }
        appointmentDao.save(appointment);
        try {
            createEventInAppointmentAudit(appointment, getAppointmentAsJsonString(appointment));
        } catch (IOException e) {
            throw new APIException(e);
        }
        return appointment;
    }

    private String getAppointmentAsJsonString(Appointment appointment) throws IOException {
        Map appointmentJson = new HashMap<String, String>();
        String serviceUuid = appointment.getService().getUuid();
        appointmentJson.put("serviceUuid", serviceUuid);
        String serviceTypeUuid = appointment.getServiceType() != null ? appointment.getServiceType().getUuid() : null;
        appointmentJson.put("serviceTypeUuid", serviceTypeUuid);
        String providerUuid = appointment.getProvider() != null ? appointment.getProvider().getUuid() : null;
        appointmentJson.put("providerUuid", providerUuid);
        String locationUuid = appointment.getLocation() != null ? appointment.getLocation().getUuid() : null;
        appointmentJson.put("locationUuid", locationUuid);
        appointmentJson.put("startDateTime", appointment.getStartDateTime().toInstant().toString());
        appointmentJson.put("endDateTime", appointment.getEndDateTime().toInstant().toString());
        appointmentJson.put("appointmentKind", appointment.getAppointmentKind().name());
        appointmentJson.put("appointmentNotes", appointment.getComments());
        ObjectMapper mapperObj = new ObjectMapper();
        return String.format("%s", mapperObj.writeValueAsString(appointmentJson));
    }

    @Override
    public List<Appointment> getAllAppointments(Date forDate) {
        List<Appointment> appointments = appointmentDao.getAllAppointments(forDate);
        return appointments.stream().filter(appointment -> !isServiceOrServiceTypeVoided(appointment)).collect(Collectors.toList());
    }

    private boolean isServiceOrServiceTypeVoided(Appointment appointment) {
        return (appointment.getService() != null && appointment.getService().getVoided()) ||
                (appointment.getServiceType() != null && appointment.getServiceType().getVoided());
    }

    @Override
    public List<Appointment> search(Appointment appointment) {
        List<Appointment> appointments = appointmentDao.search(appointment);
        return appointments.stream().filter(searchedAppointment -> !isServiceOrServiceTypeVoided(searchedAppointment)).collect(Collectors.toList());
    }

    @Override
    public List<Appointment> getAllFutureAppointmentsForService(AppointmentService appointmentService) {
        return appointmentDao.getAllFutureAppointmentsForService(appointmentService);
    }

    @Override
    public List<Appointment> getAllFutureAppointmentsForServiceType(AppointmentServiceType appointmentServiceType) {
        return appointmentDao.getAllFutureAppointmentsForServiceType(appointmentServiceType);
    }

    @Override
    public List<Appointment> getAppointmentsForService(AppointmentService appointmentService, Date startDate, Date endDate, List<AppointmentStatus> appointmentStatusList) {
        return appointmentDao.getAppointmentsForService(appointmentService, startDate, endDate, appointmentStatusList);
    }

    @Override
    public Appointment getAppointmentByUuid(String uuid) {
        Appointment appointment = appointmentDao.getAppointmentByUuid(uuid);
        return appointment;
    }

    @Override
    public void changeStatus(Appointment appointment, String status, Date onDate) throws APIException {
        AppointmentStatus appointmentStatus = AppointmentStatus.valueOf(status);
        throwExceptionIfNoProperPrivileges(appointment, appointmentStatus);
        List<String> errors = new ArrayList<>();
        validateStatusChange(appointment, appointmentStatus, errors);
        if (errors.isEmpty()) {
            appointment.setStatus(appointmentStatus);
            appointmentDao.save(appointment);
            String notes = onDate != null ? onDate.toInstant().toString() : null;
            createEventInAppointmentAudit(appointment, notes);
        } else {
            String message = StringUtils.join(errors, "\n");
            throw new APIException(message);
        }
    }

    private void throwExceptionIfNoProperPrivileges(Appointment appointment, AppointmentStatus appointmentStatus) {
        if (!validateUserAccess(appointment)) {
            throw new APIAuthenticationException(Context.getMessageSourceService().getMessage(PRIVILEGES_EXCEPTION_CODE,
                    new Object[]{MANAGE_APPOINTMENTS_PRIVILEGE}, null));
        }
        if (!isUserAllowedToResetStatus(appointmentStatus)) {
            throw new APIAuthenticationException(Context.getMessageSourceService().getMessage(PRIVILEGES_EXCEPTION_CODE,
                    new Object[]{RESET_APPOINTMENT_STATUS_PRIVILEGE}, null));
        }
    }

    private boolean isUserAllowedToResetStatus(AppointmentStatus appointmentStatus) {
        return appointmentStatus != AppointmentStatus.Scheduled || Context.hasPrivilege(RESET_APPOINTMENT_STATUS_PRIVILEGE);
    }

    @Override
    public List<Appointment> getAllAppointmentsInDateRange(Date startDate, Date endDate) {
        List<Appointment> appointments = appointmentDao.getAllAppointmentsInDateRange(startDate, endDate);
        return appointments.stream().filter(appointment -> !isServiceOrServiceTypeVoided(appointment)).collect(Collectors.toList());
    }

    @Override
    public void undoStatusChange(Appointment appointment) throws APIException {
        if (!validateUserAccess(appointment)) {
            throw new APIAuthenticationException(Context.getMessageSourceService().getMessage(PRIVILEGES_EXCEPTION_CODE,
                    new Object[]{MANAGE_APPOINTMENTS_PRIVILEGE}, null));
        }
        AppointmentAudit statusChangeEvent = appointmentAuditDao.getPriorStatusChangeEvent(appointment);
        if (statusChangeEvent != null) {
            appointment.setStatus(statusChangeEvent.getStatus());
            appointmentDao.save(appointment);
            createEventInAppointmentAudit(appointment, statusChangeEvent.getNotes());
        } else
            throw new APIException("No status change actions to undo");
    }

    @Override
    public List<Appointment> search(AppointmentSearch appointmentSearch) {
        if (isNull(appointmentSearch.getStartDate()) || isNull(appointmentSearch.getEndDate())) {
            return null;
        }
        return appointmentDao.search(appointmentSearch);
    }

    private void createEventInAppointmentAudit(Appointment appointment,
                                               String notes) {
        AppointmentAudit appointmentAuditEvent = new AppointmentAudit();
        appointmentAuditEvent.setAppointment(appointment);
        appointmentAuditEvent.setStatus(appointment.getStatus());
        appointmentAuditEvent.setNotes(notes);
        appointmentAuditDao.save(appointmentAuditEvent);
    }


    private void validateStatusChange(Appointment appointment, AppointmentStatus status, List<String> errors) {
        if (!CollectionUtils.isEmpty(statusChangeValidators)) {
            for (AppointmentStatusChangeValidator validator : statusChangeValidators) {
                validator.validate(appointment, status, errors);
            }
        }
    }
}
