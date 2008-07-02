package com.treetank.core;

import com.treetank.api.IDevice;
import com.treetank.api.IRevisionReadCore;
import com.treetank.api.RevisionReference;
import com.treetank.device.Device;

public final class RevisionReadCore implements IRevisionReadCore {

  private final IDevice mDevice1;

  private final IDevice mDevice2;

  public RevisionReadCore(final String device) {

    if ((device == null) || (device.length() < 1)) {
      throw new IllegalArgumentException(
          "Argument 'device' must not be null and longer than zero.");
    }

    mDevice1 = new Device(device + ".tt1", "r");
    mDevice2 = new Device(device + ".tt2", "r");
  }

  public final RevisionReference readRevision(final long revision) {

    if ((revision < 1)) {
      throw new IllegalArgumentException(
          "Argument 'revision' must be greater than zero.");
    }

    try {

      final byte[] buffer = new byte[64];
      final long deviceOffset = (revision << 6) + 960;
      mDevice2.read(deviceOffset, 0, 64, buffer);

      return new RevisionReference(buffer);

    } catch (Exception e) {
      throw new RuntimeException("RevisionReadCore "
          + "could not read revision due to: "
          + e.toString());
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      mDevice1.close();
      mDevice2.close();
    } finally {
      super.finalize();
    }
  }

}
