package modelo;

import java.io.Serializable;
import java.util.Objects;

public class Medico implements Serializable {

    private int idMedico;
    private String nombre;
    private String apellido;
    private String numeroMatricula;
    private Especialidad especialidad;
    private String telefono;

    public Medico(int idMedico, String nombre, String apellido, String numeroMatricula, Especialidad especialidad, String telefono) {
        if (idMedico < 0) {
            throw new IllegalArgumentException("ID de medico invalido.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de medico no puede ser vacio.");
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("Apellido de medico no puede ser vacio.");
        }
        if (numeroMatricula == null || numeroMatricula.trim().isEmpty()) {
            throw new IllegalArgumentException("Numero de matricula no puede ser vacio.");
        }
        if (especialidad == null) {
            throw new IllegalArgumentException("Especialidad de medico no puede ser nula.");
        }

        this.idMedico = idMedico;
        this.nombre = nombre;
        this.apellido = apellido;
        this.numeroMatricula = numeroMatricula;
        this.especialidad = especialidad;
        this.telefono = telefono;
    }

    public Medico(String nombre, String apellido, String numeroMatricula, Especialidad especialidad, String telefono) {
        this(0, nombre, apellido, numeroMatricula, especialidad, telefono);
    }

    public int getIdMedico() {
        return idMedico;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getNumeroMatricula() {
        return numeroMatricula;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setIdMedico(int idMedico) {
        if (idMedico <= 0) {
            throw new IllegalArgumentException("ID de medico invalido.");
        }
        this.idMedico = idMedico;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de medico no puede ser vacio.");
        }
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("Apellido de medico no puede ser vacio.");
        }
        this.apellido = apellido;
    }

    public void setNumeroMatricula(String numeroMatricula) {
        if (numeroMatricula == null || numeroMatricula.trim().isEmpty()) {
            throw new IllegalArgumentException("Numero de matricula no puede ser vacio.");
        }
        this.numeroMatricula = numeroMatricula;
    }

    public void setEspecialidad(Especialidad especialidad) {
        if (especialidad == null) {
            throw new IllegalArgumentException("Especialidad de medico no puede ser nula.");
        }
        this.especialidad = especialidad;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @Override
    public String toString() {
        return "Dr/a. " + nombre + " " + apellido + " (" + especialidad.getNombre() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Medico medico = (Medico) o;
        return idMedico == medico.idMedico && Objects.equals(numeroMatricula, medico.numeroMatricula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMedico, numeroMatricula);
    }
}
