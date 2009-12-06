package jlatexeditor.gui;

import jlatexeditor.translation.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * About dialog.
 *
 * @author Stefan Endrullis
 */
public class AboutDialog extends JFrame {
	private JPanel contentPane;
	private JLabel credits;

	public AboutDialog(String version) {
		super(I18n.getString("about_dialog.title"));
		setUndecorated(true);

		StringBuffer sb = new StringBuffer();
		sb.append(System.getProperty("java.vm.name")).append("<br>");
		sb.append(System.getProperty("java.vendor")).append("<br>");
		sb.append(System.getProperty("java.home"));
		String vmString = sb.toString();

		// setup GUI components
		contentPane = new JPanel();
		contentPane.setLayout(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));

		credits = new JLabel();
		credits.setHorizontalAlignment(2);
		credits.setHorizontalTextPosition(0);
		credits.setIcon(new ImageIcon(getClass().getResource("/logo.png")));
		if (version != null) {
			credits.setText(I18n.getString("about_dialog.text", version, System.getProperty("java.version"), vmString));
		}

		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		contentPane.add(credits, gbc);

		// set window size and location
		setSize(credits.getIcon().getIconWidth()+4, credits.getIcon().getIconHeight()+4);
		// place window in the center of the screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = getSize();
		setLocation(screenSize.width / 2 - (windowSize.width / 2), screenSize.height / 2 - (windowSize.height / 2));
		setContentPane(contentPane);
//		setModal(true);

		// add listeners
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				onClick();
			}
		});
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				onClick();
			}
		});
		addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				onClick();
			}
		});
	}

	private void onClick() {
		dispose();
	}

	public void showIt() {
		setVisible(true);
	}

	public void showAndAutoHideAfter(final int ms) {
		setAlwaysOnTop(true);
		setVisible(true);
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(ms);
				} catch (InterruptedException ignored) {}
				onClick();
			}
		}.start();
	}
}
