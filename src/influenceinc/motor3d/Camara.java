package influenceinc.motor3d;

/**
 * CAMARA ORBITAL CON PROYECCION EN PERSPECTIVA
 * ===========================================
 * Es todo el "3D" del juego: convierte un punto del mundo (x, y, z) en un
 * punto de la pantalla (sx, sy). No usa OpenGL ni librerias: solo algebra.
 *
 * 1. La camara gira alrededor de un objetivo. Su posicion sale de coordenadas
 *    esfericas: angulo horizontal (yaw), angulo vertical (pitch) y distancia.
 *
 *        ojo = objetivo + d * (cos(pitch) sen(yaw), sen(pitch), cos(pitch) cos(yaw))
 *
 * 2. Se arma una BASE ORTONORMAL de la camara con productos cruz:
 *        adelante = normalizar(objetivo - ojo)
 *        derecha  = normalizar(adelante x arribaDelMundo)
 *        arriba   = derecha x adelante
 *
 * 3. Un punto del mundo se pasa a coordenadas de camara con productos punto
 *    (es una rotacion):  cx = (p - ojo)·derecha, cy = (p - ojo)·arriba,
 *    cz = (p - ojo)·adelante   (cz es la profundidad).
 *
 * 4. PROYECCION EN PERSPECTIVA: lo lejano se ve mas pequeno porque se divide
 *    por la profundidad:
 *        sx = ancho/2 + cx * f / cz       sy = alto/2 - cy * f / cz
 *    donde f = (alto/2) / tan(fov/2) es la distancia focal.
 *
 * Es la misma matematica de Calculo III (vectores, producto punto y cruz).
 */
public class Camara {

    public static final double CERCA = 8.0;

    // estado actual (suavizado) y estado deseado
    private double yaw = 0.7, pitch = 0.42, distancia = 560;
    private double tx = 0, ty = 95, tz = 0;
    private double yawObj = yaw, pitchObj = pitch, distObj = distancia;
    private double txObj = tx, tyObj = ty, tzObj = tz;
    private double fov = Math.toRadians(55);

    // base calculada en preparar()
    private double ex, ey, ez;
    private double rx, ry, rz;
    private double ux, uy, uz;
    private double fx, fy, fz;
    private double focal;
    private int ancho, alto;

    /** Calcula el ojo, la base y la focal para este cuadro. */
    public void preparar(int w, int h) {
        ancho = w;
        alto = h;
        double cp = Math.cos(pitch);
        ex = tx + distancia * cp * Math.sin(yaw);
        ey = ty + distancia * Math.sin(pitch);
        ez = tz + distancia * cp * Math.cos(yaw);

        // adelante
        fx = tx - ex;
        fy = ty - ey;
        fz = tz - ez;
        double n = Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= n;
        fy /= n;
        fz /= n;
        // derecha = adelante x (0,1,0)
        rx = -fz;
        ry = 0;
        rz = fx;
        n = Math.sqrt(rx * rx + rz * rz);
        rx /= n;
        rz /= n;
        // arriba = derecha x adelante
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;

        focal = (h / 2.0) / Math.tan(fov / 2);
    }

    /** Mundo -> camara. out = {cx, cy, cz}. */
    public void aCamara(double x, double y, double z, double[] out) {
        double dx = x - ex, dy = y - ey, dz = z - ez;
        out[0] = dx * rx + dy * ry + dz * rz;
        out[1] = dx * ux + dy * uy + dz * uz;
        out[2] = dx * fx + dy * fy + dz * fz;
    }

    /**
     * Mundo -> pantalla. out = {sx, sy, profundidad, escala}.
     * @return false si el punto esta detras de la camara
     */
    public boolean proyectar(double x, double y, double z, double[] out) {
        aCamara(x, y, z, out);
        double cz = out[2];
        if (cz < CERCA) {
            return false;
        }
        double esc = focal / cz;
        double sx = ancho / 2.0 + out[0] * esc;
        double sy = alto / 2.0 - out[1] * esc;
        out[0] = sx;
        out[1] = sy;
        out[2] = cz;
        out[3] = esc;
        return true;
    }

    /** Camara -> pantalla, para puntos ya recortados contra el plano cercano. */
    public void camaraAPantalla(double cx, double cy, double cz, double[] out) {
        double esc = focal / cz;
        out[0] = ancho / 2.0 + cx * esc;
        out[1] = alto / 2.0 - cy * esc;
    }

    /** Interpola suavemente hacia el estado deseado. */
    public void actualizar(double dt) {
        double k = 1 - Math.exp(-dt * 5);
        yaw += (yawObj - yaw) * k;
        pitch += (pitchObj - pitch) * k;
        distancia += (distObj - distancia) * k;
        tx += (txObj - tx) * k;
        ty += (tyObj - ty) * k;
        tz += (tzObj - tz) * k;
    }

    public void girar(double dYaw, double dPitch) {
        yawObj += dYaw;
        pitchObj = Math.max(0.05, Math.min(1.45, pitchObj + dPitch));
    }

    public void acercar(double factor) {
        distObj = Math.max(140, Math.min(1100, distObj * factor));
    }

    public void setDistancia(double d) {
        distObj = Math.max(140, Math.min(1100, d));
    }

    public void mirarA(double x, double y, double z) {
        txObj = x;
        tyObj = y;
        tzObj = z;
    }

    public void configurar(double yaw, double pitch, double dist) {
        yawObj = yaw;
        pitchObj = pitch;
        distObj = dist;
    }

    /** Salta sin animacion. */
    public void fijar(double yaw, double pitch, double dist, double x, double y, double z) {
        this.yaw = yawObj = yaw;
        this.pitch = pitchObj = pitch;
        this.distancia = distObj = dist;
        tx = txObj = x;
        ty = tyObj = y;
        tz = tzObj = z;
    }

    /** Posicion del ojo en el ultimo cuadro. */
    public double[] ojo() {
        return new double[]{ex, ey, ez};
    }

    public double getYaw() {
        return yaw;
    }

    /** Componente y de la direccion "adelante" (para iluminar caras). */
    public double[] adelante() {
        return new double[]{fx, fy, fz};
    }
}
