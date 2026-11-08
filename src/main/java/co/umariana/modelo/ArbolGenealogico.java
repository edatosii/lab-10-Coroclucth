package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la Persona, el nodo del
 *    padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set (las pruebas lo
 *    revisan): para buscar una posición recorra el árbol siguiendo su camino
 *      - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre, 1 = madre).
 *        Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 *      - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 -> hijo 2, luego su hijo 3.
 *    Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con {@link ArbolInvalidoException} (un mensaje claro).
 *    Los datos inválidos de una Persona los detecta Persona.validar() (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    /** Nodo del árbol: la persona, su padre, su madre (ascendientes) y sus hijos (descendientes). */
    private static class Nodo {
        final Persona persona;
        Nodo padre;
        Nodo madre;
        final List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int tamano;

    /** Busca el nodo de una posición siguiendo su camino; null si no existe o la posición no es válida. */
    private Nodo buscarNodo(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            // los bits después del primer 1 dicen el camino: 0 = padre, 1 = madre
            String bits = Integer.toBinaryString(Integer.parseInt(posicion));
            Nodo actual = raiz;
            for (int i = 1; i < bits.length() && actual != null; i++) {
                actual = bits.charAt(i) == '0' ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                int k = Integer.parseInt(partes[i]);
                Nodo siguiente = null;
                for (Nodo hijo : actual.hijos) {
                    if (Posicion.orden(hijo.persona.getPosicion()) == k) {
                        siguiente = hijo;
                        break;
                    }
                }
                actual = siguiente;
            }
            return actual;
        }
        return null;
    }

    private int contarAscendientes(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + contarAscendientes(nodo.padre) + contarAscendientes(nodo.madre);
    }

    private int contarDescendientes(Nodo nodo) {
        int total = 1;
        for (Nodo hijo : nodo.hijos) {
            total += contarDescendientes(hijo);
        }
        return total;
    }

    private void preorden(Nodo nodo, List<Persona> destino) {
        destino.add(nodo.persona);
        for (Nodo hijo : nodo.hijos) {
            preorden(hijo, destino);
        }
    }

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser null.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1.");
        }
        this.raiz = new Nodo(sujeto);
        this.tamano = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return tamano;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     * El orden de llamada importa: primero debe existir la persona de la que depende (ver REGLAS DEL ÁRBOL).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser null.");
        }
        persona.validar();
        String pos = persona.getPosicion();
        if (Posicion.esSujeto(pos) || buscarNodo(pos) != null) {
            throw new ArbolInvalidoException("la posición " + pos + " está repetida.");
        }
        Nodo nuevo = new Nodo(persona);

        if (Posicion.esAscendiente(pos)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("la posición " + pos + " necesita que exista antes la posición "
                        + posHijo + ".");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("la posición " + pos + " debe haber nacido antes que la posición "
                        + posHijo + ".");
            }
            if (Integer.parseInt(pos) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posProgenitor = Posicion.posicionDelProgenitor(pos);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("la posición " + pos + " necesita que exista antes la posición "
                        + posProgenitor + ".");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("la posición " + pos + " debe haber nacido después que la posición "
                        + posProgenitor + ".");
            }
            // la lista de hijos queda ordenada por número de orden
            int orden = Posicion.orden(pos);
            int i = 0;
            while (i < progenitor.hijos.size()
                    && Posicion.orden(progenitor.hijos.get(i).persona.getPosicion()) < orden) {
                i++;
            }
            progenitor.hijos.add(i, nuevo);
        }
        tamano++;
        return persona;
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un ascendiente, si ese padre
     *         ya existe o si no cumple las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (buscarNodo(posicion) == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
        String pos = Posicion.posicionDelPadre(posicion);
        if (!Posicion.esAhnentafel(pos)) {
            throw new ArbolInvalidoException("Se superó el máximo de generaciones de ascendientes.");
        }
        return agregar(new Persona(pos, nombre, nacimiento, fallecimiento));
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (buscarNodo(posicion) == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar madres al sujeto y a sus ascendientes.");
        }
        String pos = Posicion.posicionDeLaMadre(posicion);
        if (!Posicion.esAhnentafel(pos)) {
            throw new ArbolInvalidoException("Se superó el máximo de generaciones de ascendientes.");
        }
        return agregar(new Persona(pos, nombre, nacimiento, fallecimiento));
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un descendiente, o si no cumple
     *         las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDAboville(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }
        int mayor = 0;
        for (Nodo hijo : progenitor.hijos) {
            mayor = Math.max(mayor, Posicion.orden(hijo.persona.getPosicion()));
        }
        return agregar(new Persona(posicion + "." + (mayor + 1), nombre, nacimiento, fallecimiento));
    }

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        int cantidad;
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
            cantidad = contarAscendientes(nodo);
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
            cantidad = contarDescendientes(nodo);
        }
        tamano -= cantidad;
        return cantidad;
    }

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente (2, 3, 4, 5...); luego los descendientes en PREORDEN (cada persona y después sus hijos:
     *         1.1, 1.1.1, 1.1.2, 1.2, ...). La lista es una copia nueva: modificarla no cambia el árbol.
     *         Este orden garantiza que cada persona aparece DESPUÉS de la que necesita (útil para guardar y cargar).
     */
    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        // recorrido por niveles (padre antes que madre): queda en orden Ahnentafel ascendente
        List<Persona> resultado = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        cola.add(raiz);
        for (int i = 0; i < cola.size(); i++) {
            Nodo actual = cola.get(i);
            if (actual != raiz) {
                resultado.add(actual.persona);
            }
            if (actual.padre != null) {
                cola.add(actual.padre);
            }
            if (actual.madre != null) {
                cola.add(actual.madre);
            }
        }
        return resultado;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> resultado = new ArrayList<>();
        for (Nodo hijo : raiz.hijos) {
            preorden(hijo, resultado);
        }
        return resultado;
    }
}
