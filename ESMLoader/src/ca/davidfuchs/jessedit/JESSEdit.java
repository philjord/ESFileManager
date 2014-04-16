package ca.davidfuchs.jessedit;

import ca.davidfuchs.jessedit.ess.ESSFile;
import ca.davidfuchs.jessedit.ess.ESSReader;
import ca.davidfuchs.jessedit.ess.ESSUtils;
import ca.davidfuchs.jessedit.ess.StructRefId;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class JESSEdit
{

	public static void main(String args[]) throws Exception
	{
		//TODO: make somethign reasonable here?
	}

	private static void dumpESSFile(String fileName) throws IOException
	{
		try
		{
			InputStream inputStream = new FileInputStream(fileName);

			System.out.println("Parsing ESS file: {}." + fileName);

			ESSFile essFile = ESSReader.readESSFile(inputStream);

			for (int index = 0; index < 10; index++)
			{
				StructRefId refId = essFile.getChangeForms().get(index).getRefId();

				for (StructRefId id : essFile.getFormIdArray())
				{
					if (refId.equals(id))
					{
						System.out.println(String.format("%d: %s -> %s", index, refId.toString(), id.toString()));
						break;
					}
				}
			}

			System.out.println(essFile.getHeader().toString());
		}
		catch (FileNotFoundException fnfex)
		{
			System.out.println("ESS file not found: {}" + fileName);
		}
	}

	private static void showScreenshot(final ESSFile essFile)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JFrame.setDefaultLookAndFeelDecorated(true);

				JFrame jFrame = new JFrame();
				jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				ImagePanel imagePanel = new ImagePanel(ESSUtils.getScreenShot(essFile));
				jFrame.getContentPane().add(imagePanel);
				jFrame.getContentPane().setPreferredSize(imagePanel.getSize());
				jFrame.pack();

				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				jFrame.setLocation(dim.width / 2 - jFrame.getSize().width / 2, dim.height / 2 - jFrame.getSize().height / 2);

				jFrame.setVisible(true);
			}
		});
	}

	private static class ImagePanel extends JPanel
	{
		private BufferedImage bufferedImage;

		public ImagePanel(BufferedImage bufferedImage)
		{
			this.bufferedImage = bufferedImage;

			setSize(bufferedImage.getWidth() + 16, bufferedImage.getHeight() + 16);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			g.drawImage(bufferedImage, 8, 8, null);
		}
	}
}
