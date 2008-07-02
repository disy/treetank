package com.treetank.util;

public final class SerialiserTool {

  public static final void writeInt(
      final int offset,
      final byte[] buffer,
      final int value) {
    buffer[offset] = (byte) (value >>> 24);
    buffer[offset + 1] = (byte) (value >>> 16);
    buffer[offset + 2] = (byte) (value >> 8);
    buffer[offset + 3] = (byte) value;
  }

  public static final void writeLong(
      final int offset,
      final byte[] buffer,
      final long value) {
    buffer[offset] = (byte) (value >>> 56);
    buffer[offset + 1] = (byte) (value >>> 48);
    buffer[offset + 2] = (byte) (value >>> 40);
    buffer[offset + 3] = (byte) (value >>> 32);
    buffer[offset + 4] = (byte) (value >>> 24);
    buffer[offset + 5] = (byte) (value >>> 16);
    buffer[offset + 6] = (byte) (value >> 8);
    buffer[offset + 7] = (byte) value;
  }

  public static final int readInt(final int offset, final byte[] buffer) {
    return ((buffer[offset] & 0xFF) << 24)
        | ((buffer[offset + 1] & 0xFF) << 16)
        | ((buffer[offset + 2] & 0xFF) << 8)
        | (buffer[offset + 3] & 0xFF);
  }

  public static final long readLong(final int offset, final byte[] buffer) {
    return (((long) buffer[offset] << 56)
        + ((long) (buffer[offset + 1] & 255) << 48)
        + ((long) (buffer[offset + 2] & 255) << 40)
        + ((long) (buffer[offset + 3] & 255) << 32)
        + ((long) (buffer[offset + 4] & 255) << 24)
        + ((buffer[offset + 5] & 255) << 16)
        + ((buffer[offset + 6] & 255) << 8) + (buffer[offset + 7] & 255));
  }

}
