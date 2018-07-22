import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

public class TrailTest {

	public static void main(String[] args) {
		// needs SwingUtilities.invokeLater() and such. Feel free to pull
		// because I'm laaaazy
		JFrame f = new JFrame();
		TrailPanel p = new TrailPanel();
		p.setOpaque(false);
		p.addMouseMotionListener(new MouseInputAdapter() {

			private Point lastPoint = null;

			@Override
			public void mouseMoved(MouseEvent e) {
				final Point mouseLocation = e.getPoint();
				if(lastPoint != null) {
					p.put(mouseLocation);
					p.connect(mouseLocation, lastPoint);
				}
				lastPoint = mouseLocation;
				p.repaint();
			}
		});
		f.setUndecorated(true);
		f.setBackground(new Color(0, 0, 0, 127));
		f.add(p);
		f.setSize(500, 500);
		// was testing stuff
		// f.setLocation(GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint());
		f.setLocation(100, 100);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}

	static Point2D[] getPoints(Line2D line) {
		List<Point2D> points = new ArrayList<>();
		Rectangle2D bounds = line.getBounds2D();
		if(bounds.getWidth() == 0) {
			for(int yy = (int) bounds.getY(); yy < bounds.getY() + bounds.getHeight(); yy++) {
				points.add(new Point((int) line.getX1(), yy));
			}
		} else if(bounds.getHeight() == 0) {
			for(int xx = (int) bounds.getX(); xx < bounds.getX() + bounds.getWidth(); xx++) {
				points.add(new Point(xx, (int) line.getY1()));
			}
		} else {
			for(int xx = (int) bounds.getX(); xx < bounds.getX() + bounds.getWidth(); xx++) {
				for(int yy = (int) bounds.getY(); yy < bounds.getY() + bounds.getHeight(); yy++) {
					Point p = new Point(xx, yy);
					if(line.ptLineDist(p) < 1) {
						points.add(p);
					}
				}
			}
		}
		return points.toArray(new Point2D[points.size()]);
	}

	// This is where the shit gets real
	private static class TrailPanel extends JPanel {

		private static final long serialVersionUID = 214019134136464119L;

		// Feel free to change data structures
		private ConcurrentHashMap<Point, Integer> points;
		private LinkedHashMap<Point, Point> lines;

		public TrailPanel() {
			points = new ConcurrentHashMap<>();
			lines = new LinkedHashMap<>();
			Thread t = new Thread(() -> {
				while(true) {
					points.replaceAll((k, v) -> v - 5);
					points.entrySet().removeIf(k -> k.getValue() < 0);
					try {
						Thread.sleep(5);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					repaint();
				}
			});
			t.start();
		}

		public void connect(Point p1, Point p2) {
			lines.put(p1, p2);
		}

		public void put(Point p) {
			points.put(p, 1020);
		}

		// Needs a better rendering policy.
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			// Don't know why I took the BF approach, the same can be done
			// without a BF and rendering it. Instead render it directly on the
			// panel. Your choice :).
			BufferedImage bf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = bf.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

			g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			for(Entry<Point, Integer> entry : points.entrySet()) {
				Point p = entry.getKey();
				try {
					g2d.setColor(new Color(255, 0, 0, entry.getValue() / 4));
				} catch(IllegalArgumentException ex) {}
				Line2D line = new Line2D.Double(p.getX(), p.getY(), lines.get(p).getX(), lines.get(p).getY());
				Point2D[] points = getPoints(line);
				for(Point2D p2d : points) {
					g2d.draw(new Arc2D.Double(p2d.getX(), p2d.getY(), 1, 1, 0, 360, Arc2D.OPEN));
				}
			}
			g.drawImage(bf, 0, 0, getWidth(), getHeight(), this);
		}
	}
}