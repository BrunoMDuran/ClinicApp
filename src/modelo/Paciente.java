package modelo;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Paciente implements Serializable {

    private int idPaciente;
    private int dni;
    private String nombre;
    private String apellido;
    private Date fechaNacimiento;
    private String telefono;
    private String correoElectronico;
    private String direccion;

    public Paciente(int idPaciente, int dni, String nombre, String apellido, Date fechaNacimiento, String telefono, String correoElectronico, String direccion) {
        if (idPaciente < 0) {
            throw new IllegalArgumentException("ID de paciente invalido.");
        }
        if (dni <= 0) {
            throw new IllegalArgumentException("DNI de paciente invalido.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de paciente no puede ser vacio.");
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("Apellido de paciente no puede ser vacio.");
        }

        this.idPaciente = idPaciente;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
        this.direccion = direccion;
    }

    public Paciente(int dni, String nombre, String apellido, Date fechaNacimiento, String telefono, String correoElectronico, String direccion) {
        this(0, dni, nombre, apellido, fechaNacimiento, telefono, correoElectronico, direccion);
    }

    public int getIdPaciente() {
        return idPaciente;
    }

    public int getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setIdPaciente(int idPaciente) {
        if (idPaciente <= 0) {
            throw new IllegalArgumentException("ID de paciente invalido.");
        }
        this.idPaciente = idPaciente;
    }

    public void setDni(int dni) {
        if (dni <= 0) {
            throw new IllegalArgumentException("DNI de paciente invalido.");
        }
        this.dni = dni;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de paciente no puede ser vacio.");
        }
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("Apellido de paciente no puede ser vacio.");
        }
        this.apellido = apellido;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String toString() {
        return "Paciente [ID=" + idPaciente + ", DNI=" + dni + ", Nombre=" + nombre + " " + apellido + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Paciente paciente = (Paciente) o;
        return idPaciente == paciente.idPaciente && dni == paciente.dni;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPaciente, dni);
    }
}
