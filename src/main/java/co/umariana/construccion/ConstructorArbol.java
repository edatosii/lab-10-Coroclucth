package co.umariana.construccion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /** Una persona leída del texto junto con el número de línea donde estaba. */
    private record Entrada(int linea, Persona persona) {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: no hay ninguna persona.");
        }

        String[] lineas = texto.split("\\r?\\n", -1);
        List<Entrada> entradas = new ArrayList<>();
        Map<String, Integer> vistas = new HashMap<>();

        for (int i = 0; i < lineas.length; i++) {
            int n = i + 1;
            if (lineas[i].isBlank()) {
                continue;
            }
            String[] campos = lineas[i].split(";", -1);
            if (campos.length < 3 || campos.length > 4) {
                throw new ArbolInvalidoException("Línea " + n
                        + ": se esperaban 3 o 4 campos separados por ';' (posicion;nombre;nacimiento;fallecimiento).");
            }
            for (int j = 0; j < campos.length; j++) {
                campos[j] = campos[j].trim();
            }

            int nacimiento;
            try {
                nacimiento = Integer.parseInt(campos[2]);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + n + ": el año de nacimiento no es un número entero.");
            }

            Integer fallecimiento = null;
            if (campos.length == 4 && !campos[3].isEmpty()) {
                try {
                    fallecimiento = Integer.valueOf(campos[3]);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException("Línea " + n
                            + ": el año de fallecimiento no es un número entero.");
                }
            }

            Persona persona = new Persona(campos[0], campos[1], nacimiento, fallecimiento);
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw new ArbolInvalidoException("Línea " + n + ": " + e.getMessage());
            }

            if (vistas.containsKey(campos[0])) {
                throw new ArbolInvalidoException("Línea " + n + ": la posición " + campos[0] + " está repetida.");
            }
            vistas.put(campos[0], n);
            entradas.add(new Entrada(n, persona));
        }

        if (entradas.isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío: no hay ninguna persona.");
        }
        if (!vistas.containsKey(Posicion.SUJETO)) {
            throw new ArbolInvalidoException("Falta el sujeto: debe existir una persona en la posición 1.");
        }

        // Orden: sujeto, ascendientes por Ahnentafel ascendente, descendientes por generación.
        entradas.sort(Comparator
                .comparingInt((Entrada e) -> grupo(e.persona().getPosicion()))
                .thenComparing((a, b) -> compararDentroDelGrupo(a.persona().getPosicion(), b.persona().getPosicion())));

        ArbolGenealogico arbol = null;
        for (Entrada e : entradas) {
            try {
                if (arbol == null) {
                    arbol = new ArbolGenealogico(e.persona());
                } else {
                    arbol.agregar(e.persona());
                }
            } catch (IllegalArgumentException ex) { // incluye ArbolInvalidoException
                throw new ArbolInvalidoException("Línea " + e.linea() + ": " + ex.getMessage());
            }
        }
        return arbol;
    }

    private static int grupo(String posicion) {
        if (Posicion.esSujeto(posicion)) return 0;
        if (Posicion.esAscendiente(posicion)) return 1;
        return 2;
    }

    private static int compararDentroDelGrupo(String a, String b) {
        if (Posicion.esAhnentafel(a)) { // ambos son ascendientes o sujeto
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        int porGeneracion = Integer.compare(Posicion.generacion(a), Posicion.generacion(b));
        if (porGeneracion != 0) return porGeneracion;
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 1; i < pa.length; i++) {
            int c = Integer.compare(Integer.parseInt(pa[i]), Integer.parseInt(pb[i]));
            if (c != 0) return c;
        }
        return 0;
    }
}
