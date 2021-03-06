/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009, 2010 Wayne Meissner
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.cext;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link GIL} keeps locks for Threads running native code. Only one Thread can usually
 * be running C code at a time.
 */
final class GIL {

    private static final ReentrantLock lock = new ReentrantLock();

    private GIL() {
    }

    public static void acquire() {
        lock.lock();
    }

    /**
     * Acquire the lock n-times. This method is used to implement {@link JRuby#nativeBlockingRegion}.
     * After finishing execution of unmanaged code, the executing thread has to re-acquire all previously
     * owned locks (for cases where the execution of the Thread went through Java->C->Java->C multiple times)
     */
    public static void acquire(int locks) {
        for(int i = 0; i < locks; i++) {
            acquire();
        }
    }

    /**
     * Decrease the lock holding count by one, and do a {@link GC} run if this is
     * the last lock held by this thread.
     */
    public static void release() {
        try {
            if (lock.getHoldCount() == 1) {
                GC.cleanup();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Fast unlocking without GC.
     */
    public static void releaseNoCleanup() {
        lock.unlock();
    }

    /**
     * Release all locks currently held by this thread.
     * @return the unlock count
     */
    public static int releaseAllLocks() {
        int i;
        for(i = 0; i < lock.getHoldCount(); i++) {
            releaseNoCleanup();
        }
        return ++i;
    }
}
