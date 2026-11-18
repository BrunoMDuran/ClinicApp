package modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class HistoriaClinica implements Serializable {

    private int idHistoria;
    private Paciente paciente;
    private LocalDateTime fechaAtencion;
    private String diagnostico;
    private String tratamiento;
    private String notas;

    public HistoriaClinica(int idHistoria, Paciente paciente, LocalDateTime fechaAtencion, String diagnostico, String tratamiento, String notas) {
        if (idHistoria < 0) {
            throw new IllegalArgumentException("ID de historia clinica invalido.");
        }
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no puede ser nulo.");
        }
        if (fechaAtencion == null) {
            throw new IllegalArgumentException("Fecha de atencion no puede ser nula.");
        }

        this.idHistoria = idHistoria;
        this.paciente = paciente;
        this.fechaAtencion = fechaAtencion;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.notas = notas;
    }

    public HistoriaClinica(Paciente paciente, LocalDateTime fechaAtencion, String diagnostico, String tratamiento, String notas) {
        this(0, paciente, fechaAtencion, diagnostico, tratamiento, notas);
    }

    public int getIdHistoria() {
        return idHistoria;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public LocalDateTime getFechaAtencion() {
        return fechaAtencion;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public String getNotas() {
        return notas;
    }

    public void setIdHistoria(int idHistoria) {
        if (idHistoria <= 0) {
            throw new IllegalArgumentException("ID de historia clinica invalido.");
        }
        this.idHistoria = idHistoria;
    }

    public void setPaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no puede ser nulo.");
        }
        this.paciente = paciente;
    }

    public void setFechaAtencion(LocalDateTime fechaAtencion) {
        if (fechaAtencion == null) {
            throw new IllegalArgumentException("Fecha de atencion no puede ser nula.");
        }
        this.fechaAtencion = fechaAtencion;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return "Historia Clinica [ID=" + idHistoria + ", Paciente=" + paciente.getNombre() + ", Fecha=" + fechaAtencion + ", Diagnostico=" + diagnostico + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HistoriaClinica that = (HistoriaClinica) o;
        return idHistoria == that.idHistoria;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idHistoria);
    }
}
