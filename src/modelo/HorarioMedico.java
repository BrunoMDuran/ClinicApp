package modelo;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;

public class HorarioMedico implements Serializable {

    private int idHorario;
    private Medico medico;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public HorarioMedico(int idHorario, Medico medico, String diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        if (idHorario < 0) {
            throw new IllegalArgumentException("ID de horario invalido.");
        }
        if (medico == null) {
            throw new IllegalArgumentException("Medico no puede ser nulo.");
        }
        if (diaSemana == null || diaSemana.trim().isEmpty()) {
            throw new IllegalArgumentException("Dia de la semana no puede ser vacio.");
        }
        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("Horas de inicio/fin no pueden ser nulas.");
        }
        if (horaInicio.isAfter(horaFin)) {
            throw new IllegalArgumentException("Hora de inicio no puede ser despues de hora fin.");
        }

        this.idHorario = idHorario;
        this.medico = medico;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public HorarioMedico(Medico medico, String diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this(0, medico, diaSemana, horaInicio, horaFin);
    }

    public int getIdHorario() {
        return idHorario;
    }

    public Medico getMedico() {
        return medico;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setIdHorario(int idHorario) {
        if (idHorario <= 0) {
            throw new IllegalArgumentException("ID de horario invalido.");
        }
        this.idHorario = idHorario;
    }

    public void setMedico(Medico medico) {
        if (medico == null) {
            throw new IllegalArgumentException("Medico no puede ser nulo.");
        }
        this.medico = medico;
    }

    public void setDiaSemana(String diaSemana) {
        if (diaSemana == null || diaSemana.trim().isEmpty()) {
            throw new IllegalArgumentException("Dia de la semana no puede ser vacio.");
        }
        this.diaSemana = diaSemana;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        if (horaInicio == null) {
            throw new IllegalArgumentException("Hora de inicio no puede ser nula.");
        }
        this.horaInicio = horaInicio;
    }

    public void setHoraFin(LocalTime horaFin) {
        if (horaFin == null) {
            throw new IllegalArgumentException("Hora de fin no puede ser nula.");
        }
        this.horaFin = horaFin;
    }

    @Override
    public String toString() {
        return diaSemana + " " + horaInicio + "-" + horaFin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HorarioMedico that = (HorarioMedico) o;
        return idHorario == that.idHorario
                && Objects.equals(medico, that.medico)
                && Objects.equals(diaSemana, that.diaSemana)
                && Objects.equals(horaInicio, that.horaInicio)
                && Objects.equals(horaFin, that.horaFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idHorario, medico, diaSemana, horaInicio, horaFin);
    }
}
