/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexectask;

import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.task.annotations.Resource;
import org.openmole.plugin.resource.virtual.IVirtualMachine;
import org.openmole.plugin.resource.virtual.IVirtualMachinePool;
import org.openmole.plugin.resource.virtual.VirtualMachineResource;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualSystemExecTask extends Task {

    @Resource
    final VirtualMachineResource virtualMachineResource;

    public VirtualSystemExecTask(String name, VirtualMachineResource virtualMachineResource) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.virtualMachineResource = virtualMachineResource;
    }

    @Override
    protected void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {
        System.out.println("Execute virtual task.");
        IVirtualMachinePool pool = virtualMachineResource.getVirtualMachineShared();
        IVirtualMachine virtualMachine = pool.borrowAVirtualMachine();
        pool.returnVirtualMachine(virtualMachine);
         System.out.println("Executed virtual task.");
    }

}
