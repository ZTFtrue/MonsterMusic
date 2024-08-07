/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Christian Laireiter <liree@web.de>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio.exceptions;


/**
 * This exception is thrown if a
 * {@link org.jaudiotagger.audio.generic.AudioFileModificationListener} wants to
 * prevent; from actually finishing its
 * operation.<br>
 * This exception can be used in all methods but
 * {@link org.jaudiotagger.audio.generic.AudioFileModificationListener#fileOperationFinished(java.io.File)}.
 *
 * @author Christian Laireiter
 */
public class ModifyVetoException extends Exception
{

    /**
     * (overridden)
     */
    public ModifyVetoException()
    {
        super();
    }

    /**
     * (overridden)
     *
     * @param message
     * @see Exception#Exception(java.lang.String)
     */
    public ModifyVetoException(String message)
    {
        super(message);
    }

    /**
     * (overridden)
     *
     * @param message
     * @param cause
     * @see Exception#Exception(java.lang.String,java.lang.Throwable)
     */
    public ModifyVetoException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * (overridden)
     *
     * @param cause
     * @see Exception#Exception(java.lang.Throwable)
     */
    public ModifyVetoException(Throwable cause)
    {
        super(cause);
    }

}
