/**
 * sacmis
 * An application wich executes PHP code and displays the result. Useful for
 * testing and debugging PHP scripts.
 * 
 * Copyright (c) 2010-2015 Christoph Kappestein <k42b3.x@gmail.com>
 * 
 * This file is part of sacmis. sacmis is free software: you can 
 * redistribute it and/or modify it under the terms of the GNU 
 * General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or at any later version.
 * 
 * sacmis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with sacmis. If not, see <http://www.gnu.org/licenses/>.
 */

package com.k42b3.sacmis;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.k42b3.sacmis.MenuBar.MenuBarActionListener;
import com.k42b3.sacmis.TemplateManager.Package;
import com.k42b3.sacmis.TemplateManager.Template;
import com.k42b3.sacmis.executor.Composer;
import com.k42b3.sacmis.executor.Php;
import com.k42b3.sacmis.executor.PhpUnit;

/**
 * Sacmis
 *
 * @author  Christoph Kappestein <k42b3.x@gmail.com>
 * @license http://www.gnu.org/licenses/gpl.html GPLv3
 * @link    https://github.com/k42b3/sacmis
 */
public class Sacmis extends JFrame
{
	public static final String VERSION = "0.2.0";

	protected Logger logger = Logger.getLogger("com.k42b3.sacmis");

	protected int exitCode = 0;
	protected boolean writeStdIn = false;
	protected long timeout = 30000;
	protected String inputCache = "input-%num%.php";

	protected JTabbedPane tp;

	protected ByteArrayOutputStream baos;
	protected ByteArrayOutputStream baosErr;
	protected ByteArrayInputStream bais;

	private Queue<String> commandQueue;
	
	public Sacmis() throws Exception
	{
		// settings
		this.setTitle("Sacmis (version: " + VERSION + ")");
		this.setLocation(100, 100);
		this.setSize(800, 600);
		this.setMinimumSize(this.getSize());

		// set toolbar
		this.setJMenuBar(this.buildMenuBar());

		// main panel
		tp = new JTabbedPane();
		tp.addChangeListener(new ChangeListener(){

			public void stateChanged(ChangeEvent e)
			{
				getActiveIn().requestFocusInWindow();
			}

		});

		this.add(tp, BorderLayout.CENTER);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// add tab
		this.newTab();
	}

	protected JMenuBar buildMenuBar()
	{
		MenuBar menuBar = new MenuBar();
		menuBar.setActionListener(new MenuBarActionListener(){

			public void onActionRun()
			{
				onRun();
			}

			public void onActionReset()
			{
				onReset();
			}

			public void onActionSave()
			{
				saveFile();
			}
			
			public void onActionLoad()
			{
				loadFile();
			}

			public void onActionNewTab()
			{
				newTab();
			}

			public void onActionCloseTab()
			{
				closeTab();
			}
			
			public void onActionAbout()
			{
				onAbout();
			}

			public void onActionExit()
			{
				onExit();
			}

			public void onComposerOpen()
			{
				onOpen();
			}

			public void onComposerUpdate()
			{
				onUpdate();
			}

			public void onComposerRequire()
			{
				onRequire();
			}

			public void onPhpUnitTest()
			{
				onTest();
			}

			public void onPhpOpcode()
			{
				onOpcode();
			}

			public void onTemplateLoad(String name)
			{
				onTemplate(name);
			}

		});

		return menuBar;
	}

	protected JComponent buildMainPanel(int num)
	{
		// textareas
		JPanel panelMain = new JPanel();
		panelMain.setLayout(new BorderLayout());
		panelMain.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		// in textarea
		RTextScrollPane scrIn = new RTextScrollPane(new InTextArea());
		scrIn.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		scrIn.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrIn.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrIn.setPreferredSize(new Dimension(600, 200));

		sp.add(scrIn);
		
		// out textarea
		RTextScrollPane scrOut = new RTextScrollPane(new OutTextArea());
		scrOut.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		scrOut.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrOut.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	

		sp.add(scrOut);

		panelMain.add(sp, BorderLayout.CENTER);
		
		JPanel panel = new JPanel(new BorderLayout());
		//panel.add(panelNorth, BorderLayout.NORTH);
		panel.add(panelMain, BorderLayout.CENTER);

		return panel;
	}

	protected Container getActivePanel()
	{
		return (Container) tp.getSelectedComponent();
	}

	protected InTextArea getActiveIn()
	{
		Container comp = (Container) this.getActivePanel().getComponent(0);
		JSplitPane sp = (JSplitPane) comp.getComponent(0);
		RTextScrollPane scp = (RTextScrollPane) sp.getComponent(1);
		JViewport vp = (JViewport) scp.getComponent(0);
		InTextArea in = (InTextArea) vp.getComponent(0);

		return in;
	}
	
	protected OutTextArea getActiveOut()
	{
		Container comp = (Container) this.getActivePanel().getComponent(0);
		JSplitPane sp = (JSplitPane) comp.getComponent(0);
		RTextScrollPane scp = (RTextScrollPane) sp.getComponent(2);
		JViewport vp = (JViewport) scp.getComponent(0);
		OutTextArea out = (OutTextArea) vp.getComponent(0);

		return out;
	}

	protected String getInputFile()
	{
		return inputCache.replaceAll("%num%", "" + tp.getSelectedIndex());
	}

	protected void loadFile()
	{
		File fIn = new File(this.getInputFile());
		FileInputStream fileIn;
	
		this.getActiveIn().setText("");
		this.getActiveOut().setText("");
	
		if(fIn.exists())
		{
			logger.info("Load file " + fIn.getName());

			try
			{
				fileIn = new FileInputStream(fIn);
				BufferedReader brIn = new BufferedReader(new InputStreamReader(fileIn));
				String line = null;

				while((line = brIn.readLine()) != null)
				{
					this.getActiveIn().append(line + "\n");
				}

				brIn.close();
			}
			catch(IOException e)
			{
				logger.error(e.getMessage(), e);

				this.getActiveOut().setText(e.getMessage());
			}
		}
	}

	protected void saveFile()
	{
		try
		{
			FileOutputStream fileOut;
			fileOut = new FileOutputStream(this.getInputFile());

		    new PrintStream(fileOut).print(this.getActiveIn().getText());

		    fileOut.close();
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);

			this.getActiveOut().setText(e.getMessage());
		}
	}

	protected void newTab()
	{
		tp.addTab("Tab-" + (tp.getTabCount()), this.buildMainPanel(tp.getTabCount()));
		tp.setSelectedIndex(tp.getTabCount() - 1);

		// load file
		this.loadFile();
		
		// focus
		getActiveIn().requestFocusInWindow();
	}

	protected void closeTab()
	{
		if(tp.getTabCount() > 1)
		{
			tp.remove(tp.getSelectedIndex());
		}
	}

	protected void executeCommand(String input, final int num, boolean writeStdIn)
	{
		this.getActiveOut().setText("");
		String cmd = commandQueue.poll();

		try
		{
			// replace input cache file
			logger.info("Execute-" + num + ": " + cmd);

			// save file
			saveFile();

			// parse cmd
			CommandLine commandLine = CommandLine.parse(cmd);

			// set timeout
			ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);

			// create executor
			DefaultExecutor executor = new DefaultExecutor();
			executor.setExitValue(this.exitCode);

			this.baos = new ByteArrayOutputStream();
			this.baosErr = new ByteArrayOutputStream();

			if(writeStdIn)
			{
				this.bais = new ByteArrayInputStream(input.getBytes());

				executor.setStreamHandler(new PumpStreamHandler(this.baos, this.baosErr, this.bais));
			}
			else
			{
				executor.setStreamHandler(new PumpStreamHandler(this.baos, this.baosErr));
			}

			executor.setWatchdog(watchdog);
			executor.execute(commandLine, new ExecuteResultHandler(){

				public void onProcessComplete(int e) 
				{
					if(commandQueue.size() > 0)
					{
						executeCommand(baos.toString(), num, true);
					}
					else
					{
						setOutput(baos, baosErr);
					}
				}

				public void onProcessFailed(ExecuteException e) 
				{
					setOutput(baosErr, baos);
				}

				protected void setOutput(ByteArrayOutputStream out1, ByteArrayOutputStream out2)
				{
					String output = out1.toString();
					if(!output.isEmpty())
					{
						getActiveOut().setText(output);
					}
					else
					{
						output = out2.toString();
						if(!output.isEmpty())
						{
							getActiveOut().setText(output);
						}
					}
				}

			});
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);

			this.getActiveOut().setText(e.getMessage());
		}
	}

	protected void onRun()
	{
		commandQueue = new LinkedList<String>();
		commandQueue.add("php " + this.getInputFile());

		executeCommand(this.getActiveIn().getText(), tp.getSelectedIndex(), this.writeStdIn);
	}
	
	protected void onReset()
	{
		this.getActiveIn().setText("");
		this.getActiveOut().setText("");

		loadFile();
	}

	protected void onAbout()
	{
		StringBuilder text = new StringBuilder();
		text.append("Version: sacmis " + VERSION + "\n");
		text.append("Author: Christoph \"k42b3\" Kappestein" + "\n");
		text.append("Website: https://github.com/k42b3/sacmis" + "\n");
		text.append("License: GPLv3 <http://www.gnu.org/licenses/gpl-3.0.html>" + "\n");
		text.append("\n");
		text.append("An application wich executes PHP code and displays the result. Useful for" + "\n");
		text.append("testing and debugging PHP scripts." + "\n");

		this.getActiveOut().setText(text.toString());
	}

	protected void onExit()
	{
		saveFile();

		System.exit(0);
	}
	
	/**
	 * We copy the selected composer.json to our current working dir and execute 
	 * an composer install
	 */
	protected void onOpen()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(new JsonFilter());

		int returnValue = fileChooser.showOpenDialog(this);

		if(returnValue == JFileChooser.APPROVE_OPTION)
		{
			File selectedFile = fileChooser.getSelectedFile();
			File destFile = new File("composer.json");

			try
			{
				FileUtils.copyFile(selectedFile, destFile);

				// remove composer lock
				File lock = new File("composer.lock");
				if(lock.isFile())
				{
					lock.delete();
				}

				new Thread(new Composer("install", getActiveOut())).start();
			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	protected void onUpdate()
	{
		try
		{
			new Thread(new Composer("update", getActiveOut())).start();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void onRequire()
	{
		try
		{
			String require = JOptionPane.showInputDialog("Please enter the required package with a version constraint,\ne.g. foo/bar:1.0.0 or foo/bar=1.0.0 or \"foo/bar 1.0.0\"");

			if(require != null && !require.isEmpty())
			{
				new Thread(new Composer("require " + require, getActiveOut())).start();	
			}
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void onTest()
	{
		try
		{
			this.saveFile();

			new Thread(new PhpUnit(this.getInputFile(), getActiveOut())).start();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void onOpcode()
	{
		try
		{
			this.saveFile();

			if(ExecutorAbstract.hasExecutable("php --re vld", "vld version"))
			{
				new Thread(new Php("-d vld.active=1 -d vld.execute=0 -f" + this.getInputFile(), getActiveOut())).start();
			}
			else
			{
				throw new Exception("Looks like the PHP vld extension is not installed");
			}
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void onTemplate(String name)
	{
		try
		{
			TemplateManager manager = new TemplateManager();
			Template template = manager.getTemplate(name);
			
			if(template != null)
			{
				ArrayList<Package> packages = template.getRequires();
				if(packages.size() > 0)
				{
					StringBuilder cmd = new StringBuilder();
					for(int i = 0; i < packages.size(); i++)
					{
						cmd.append(packages.get(i).getName());
						cmd.append("=");
						cmd.append(packages.get(i).getVersion());
						cmd.append(" ");
					}

					new Thread(new Composer("require " + cmd.toString(), getActiveOut())).start();	
				}

				this.getActiveIn().setText(template.getSource());
			}
			else
			{
				throw new Exception("Could not find template");
			}
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Information", JOptionPane.ERROR_MESSAGE);
		}
	}

	class JsonFilter extends FileFilter
	{
		public boolean accept(File pathname)
		{
			return pathname.isDirectory() || pathname.getName().endsWith(".json");
		}

		public String getDescription()
		{
			return "composer.json";
		}
	}
}
