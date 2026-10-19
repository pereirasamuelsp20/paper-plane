import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Java reference engine for Paper Flight.
 *
 * The browser edition renders the game with HTML canvas and runs its live loop
 * in JavaScript. This Java class demonstrates the same core ideas for a
 * presentation or a future Java/Swing port: survival scoring, gust influence,
 * right-edge incoming projectiles, lives, and collision handling.
 */
public final class PaperFlightEngine {
    public static final double WORLD_WIDTH = 960.0;
    public static final double WORLD_HEIGHT = 600.0;
    public static final double SCORE_PER_SECOND = 10.0;
    private static final int MAX_INCOMING_PROJECTILES = 72;

    private static final class Projectile {
        double x;
        double y;
        double vx;
        double vy;
        double radius;
        double age;

        Projectile(double x, double y, double vx, double vy, double radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }
    }

    private final List<Projectile> incoming = new ArrayList<>();
    private double planeX = 145.0;
    private double planeY = WORLD_HEIGHT / 2.0;
    private double planeVx;
    private double planeVy;
    private double elapsedSeconds;
    private double invulnerableSeconds;
    private int lives = 3;
    private boolean running = true;

    /**
     * Advance the simulation. Inputs should be in the range -1.0 to 1.0.
     * Wind values represent gust acceleration in pixels per second squared.
     */
    public void update(double deltaSeconds, double horizontalInput,
                       double verticalInput, double windX, double windY) {
        if (!running) {
            return;
        }

        double dt = clamp(deltaSeconds, 0.0, 0.05);
        if (dt == 0.0) {
            return;
        }

        elapsedSeconds += dt;
        invulnerableSeconds = Math.max(0.0, invulnerableSeconds - dt);

        double inputX = clamp(horizontalInput, -1.0, 1.0);
        double inputY = clamp(verticalInput, -1.0, 1.0);
        double inputLength = Math.hypot(inputX, inputY);
        if (inputLength > 1.0) {
            inputX /= inputLength;
            inputY /= inputLength;
        }

        final double acceleration = 580.0;
        planeVx += inputX * acceleration * dt + windX * 0.82 * dt;
        planeVy += inputY * acceleration * dt + windY * 1.15 * dt;
        double drag = Math.pow(0.035, dt);
        planeVx = clamp(planeVx * drag, -260.0, 260.0);
        planeVy = clamp(planeVy * drag, -285.0, 285.0);
        planeX = clamp(planeX + planeVx * dt, 48.0, WORLD_WIDTH - 55.0);
        planeY = clamp(planeY + planeVy * dt, 44.0, WORLD_HEIGHT - 45.0);

        updateIncomingProjectiles(dt, windX, windY);
        checkCollisions();
    }

    /** Adds a hostile projectile outside the right edge; the player cannot fire. */
    public void launchProjectileFromRight(double sourceY, double verticalVelocity,
                                          double radius) {
        if (!running) {
            return;
        }
        if (incoming.size() >= MAX_INCOMING_PROJECTILES) {
            incoming.remove(0);
        }
        double y = clamp(sourceY, 28.0, WORLD_HEIGHT - 28.0);
        incoming.add(new Projectile(
                WORLD_WIDTH + 7.0,
                y,
                -245.0,
                clamp(verticalVelocity, -240.0, 240.0),
                clamp(radius, 2.0, 9.0)));
    }

    private void updateIncomingProjectiles(double dt, double windX, double windY) {
        Iterator<Projectile> iterator = incoming.iterator();
        while (iterator.hasNext()) {
            Projectile shot = iterator.next();
            shot.vx += windX * 0.11 * dt;
            shot.vy += windY * 0.15 * dt;
            // Wind may bend a shot, but hostile shots always keep moving left.
            shot.vx = -Math.max(105.0, Math.abs(shot.vx));
            shot.x += shot.vx * dt;
            shot.y += shot.vy * dt;
            shot.age += dt;
            if (shot.age > 5.0 || shot.x < -50.0 || shot.y < -50.0
                    || shot.y > WORLD_HEIGHT + 50.0) {
                iterator.remove();
            }
        }
    }

    private void checkCollisions() {
        if (invulnerableSeconds > 0.0) {
            return;
        }
        Iterator<Projectile> iterator = incoming.iterator();
        while (iterator.hasNext()) {
            Projectile shot = iterator.next();
            if (Math.hypot(planeX - shot.x, planeY - shot.y) < 15.0 + shot.radius) {
                iterator.remove();
                lives = Math.max(0, lives - 1);
                invulnerableSeconds = 1.35;
                planeVx -= 80.0;
                if (lives == 0) {
                    running = false;
                }
                return;
            }
        }
    }

    /** Shared scoring function used by both the simulation and live HTTP API. */
    public static int scoreForElapsed(double seconds) {
        if (!Double.isFinite(seconds)) {
            return 0;
        }
        double safeSeconds = Math.max(0.0, Math.min(seconds, 86_400.0));
        return (int) Math.floor(safeSeconds * SCORE_PER_SECOND);
    }

    /** Survival-only score: 10 points for every second alive. */
    public int getScore() {
        return scoreForElapsed(elapsedSeconds);
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameOver() {
        return !running;
    }

    public double getPlaneX() {
        return planeX;
    }

    public double getPlaneY() {
        return planeY;
    }

    public int getIncomingProjectileCount() {
        return incoming.size();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /** Small console demo so this reference class can be compiled and run. */
    public static void main(String[] args) {
        PaperFlightEngine game = new PaperFlightEngine();
        final double step = 1.0 / 60.0;
        for (int frame = 0; frame < 600 && !game.isGameOver(); frame++) {
            double time = frame * step;
            if (frame % 110 == 0) {
                game.launchProjectileFromRight(
                        game.getPlaneY() + Math.sin(time) * 120.0,
                        Math.cos(time) * 38.0,
                        4.5);
            }
            double gentleSteer = Math.sin(time * 0.75) * 0.22;
            double gustX = time % 7.0 > 3.0 ? -28.0 : 12.0;
            double gustY = Math.sin(time * 2.1) * 52.0;
            game.update(step, 0.0, gentleSteer, gustX, gustY);
        }

        System.out.println("Paper Flight Java reference demo");
        System.out.println("Survival time: " + String.format("%.2f", game.getElapsedSeconds()) + " s");
        System.out.println("Survival score: " + game.getScore());
        System.out.println("Hull remaining: " + game.getLives());
        System.out.println("State: " + (game.isGameOver() ? "GAME OVER" : "FLYING"));
    }
}
