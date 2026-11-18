package modelo;

import java.io.Serializable;
import java.util.Objects;

public class Usuario implements Serializable {

    private int idUsuario;
    private String nombreUsuario;
    private String contrasenaHash;
    private String rol;
    private Integer idMedicoAsociado;

    public Usuario(int idUsuario, String nombreUsuario, String contrasenaHash, String rol, Integer idMedicoAsociado) {
        if (idUsuario < 0) {
            throw new IllegalArgumentException("ID de usuario invalido.");
        }
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de usuario no puede ser vacio.");
        }
        if (contrasenaHash == null || contrasenaHash.isEmpty()) {
            throw new IllegalArgumentException("Contrasena no puede ser vacia.");
        }
        if (rol == null || rol.trim().isEmpty()) {
            throw new IllegalArgumentException("Rol no puede ser vacio.");
        }
        if (!rol.equals("Administrador") && !rol.equals("Secretario_Admin") && !rol.equals("Medico")) {
            throw new IllegalArgumentException("Rol invalido.");
        }

        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.idMedicoAsociado = idMedicoAsociado;
    }

    public Usuario(String nombreUsuario, String contrasenaHash, String rol, Integer idMedicoAsociado) {
        this(0, nombreUsuario, contrasenaHash, rol, idMedicoAsociado);
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public String getRol() {
        return rol;
    }

    public Integer getIdMedicoAsociado() {
        return idMedicoAsociado;
    }

    public void setIdUsuario(int idUsuario) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("ID de usuario invalido.");
        }
        this.idUsuario = idUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de usuario no puede ser vacio.");
        }
        this.nombreUsuario = nombreUsuario;
    }

    public void setContrasenaHash(String contrasenaHash) {
        if (contrasenaHash == null || contrasenaHash.isEmpty()) {
            throw new IllegalArgumentException("Contrasena no puede ser vacia.");
        }
        this.contrasenaHash = contrasenaHash;
    }

    public void setRol(String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            throw new IllegalArgumentException("Rol no puede ser vacio.");
        }
        if (!rol.equals("Administrador") && !rol.equals("Secretario_Admin") && !rol.equals("Medico")) {
            throw new IllegalArgumentException("Rol invalido.");
        }
        this.rol = rol;
    }

    public void setIdMedicoAsociado(Integer idMedicoAsociado) {
        this.idMedicoAsociado = idMedicoAsociado;
    }

    @Override
    public String toString() {
        return "Usuario [ID=" + idUsuario + ", Nombre=" + nombreUsuario + ", Rol=" + rol + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Usuario usuario = (Usuario) o;
        return idUsuario == usuario.idUsuario && Objects.equals(nombreUsuario, usuario.nombreUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, nombreUsuario);
    }
}
