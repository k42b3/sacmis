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

import javax.swing.UIManager;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Entry
 *
 * @author  Christoph Kappestein <k42b3.x@gmail.com>
 * @license http://www.gnu.org/licenses/gpl.html GPLv3
 * @link    https://github.com/k42b3/sacmis
 */
public class Entry 
{
	public static void main(String[] args)
	{
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("com.k42b3.sacmis").addAppender(new ConsoleAppender(new PatternLayout()));

        try
        {
    		String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
        	UIManager.setLookAndFeel(lookAndFeel);

        	// start sacmis
        	Sacmis win = new Sacmis();
        	win.setVisible(true);
        }
        catch(Exception e)
        {
        	Logger.getLogger("com.k42b3.sacmis").error(e.getMessage(), e);
        }
	}
}
