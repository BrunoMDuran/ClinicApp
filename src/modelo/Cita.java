package modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Objects;

public class Cita implements Serializable {

    private int idCita;
    private Paciente paciente;
    private Medico medico;
    private LocalDate fechaCita;
    private LocalTime horaCita;
    private String motivo;
    private String estado;

    public Cita(int idCita, Paciente paciente, Medico medico, LocalDate fechaCita, LocalTime horaCita, String motivo, String estado) {
        if (idCita < 0) {
            throw new IllegalArgumentException("ID de cita invalido.");
        }
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no puede ser nulo.");
        }
        if (medico == null) {
            throw new IllegalArgumentException("Medico no puede ser nulo.");
        }
        if (fechaCita == null) {
            throw new IllegalArgumentException("Fecha de cita no puede ser nula.");
        }
        if (horaCita == null) {
            throw new IllegalArgumentException("Hora de cita no puede ser nula.");
        }
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado de cita no puede ser vacio.");
        }

        this.idCita = idCita;
        this.paciente = paciente;
        this.medico = medico;
        this.fechaCita = fechaCita;
        this.horaCita = horaCita;
        this.motivo = motivo;
        this.estado = estado;
    }

    public Cita(Paciente paciente, Medico medico, LocalDate fechaCita, LocalTime horaCita, String motivo) {
        this(0, paciente, medico, fechaCita, horaCita, motivo, "Agendada");
    }

    public int getIdCita() {
        return idCita;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public LocalDate getFechaCita() {
        return fechaCita;
    }

    public LocalTime getHoraCita() {
        return horaCita;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setIdCita(int idCita) {
        if (idCita <= 0) {
            throw new IllegalArgumentException("ID de cita invalido.");
        }
        this.idCita = idCita;
    }

    public void setPaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no puede ser nulo.");
        }
        this.paciente = paciente;
    }

    public void setMedico(Medico medico) {
        if (medico == null) {
            throw new IllegalArgumentException("Medico no puede ser nulo.");
        }
        this.medico = medico;
    }

    public void setFechaCita(LocalDate fechaCita) {
        if (fechaCita == null) {
            throw new IllegalArgumentException("Fecha de cita no puede ser nula.");
        }
        this.fechaCita = fechaCita;
    }

    public void setHoraCita(LocalTime horaCita) {
        if (horaCita == null) {
            throw new IllegalArgumentException("Hora de cita no puede ser nula.");
        }
        this.horaCita = horaCita;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public synchronized void setEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado de cita no puede ser vacio.");
        }
        List<String> estadosValidos = new ArrayList<>(Arrays.asList("Agendada", "Atendida", "Cancelada", "No Asistida", "En Proceso"));
        if (!estadosValidos.contains(estado)) {
            throw new IllegalArgumentException("Estado de cita invalido: " + estado);
        }
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Cita [ID=" + idCita + ", Paciente=" + paciente.getNombre() + ", Medico=" + medico.getApellido() + ", Fecha=" + fechaCita + ", Hora=" + horaCita + ", Estado=" + estado + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cita cita = (Cita) o;
        return idCita == cita.idCita;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCita);
    }
}
