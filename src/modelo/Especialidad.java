package modelo;

import java.io.Serializable;
import java.util.Objects;

public class Especialidad implements Serializable {

    private int id;
    private String nombre;

    public Especialidad(int id, String nombre) {
        if (id < 0) {
            throw new IllegalArgumentException("ID de especialidad invalido.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de especialidad no puede ser vacio.");
        }
        this.id = id;
        this.nombre = nombre;
    }

    public Especialidad(String nombre) {
        this(0, nombre);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de especialidad invalido.");
        }
        this.id = id;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de especialidad no puede ser vacio.");
        }
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Especialidad that = (Especialidad) o;
        return id == that.id && Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre);
    }
}
