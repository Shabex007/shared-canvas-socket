import java.awt.*;
import java.io.Serializable;

public class DrawCommand implements Serializable {
    public Point start;
    public Point end;
    public Color color;
    public String username;
    public boolean clear;

    public DrawCommand(Point start, Point end, Color color, String username, boolean clear) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.username = username;
        this.clear = clear;
    }
}