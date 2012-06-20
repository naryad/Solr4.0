package org.apache.lucene.util.packed;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;

/**
 * This class is similar to {@link Packed64} except that it trades space for
 * speed by ensuring that a single block needs to be read/written in order to
 * read/write a value.
 */
final class Packed64SingleBlock extends PackedInts.MutableImpl {

  private static final int[] SUPPORTED_BITS_PER_VALUE = new int[] {1, 2, 3, 4,
      5, 6, 7, 8, 9, 10, 12, 16, 21, 32};
  static final int MAX_SUPPORTED_BITS_PER_VALUE = 32;
  private static final long[][] WRITE_MASKS = new long[MAX_SUPPORTED_BITS_PER_VALUE+1][];
  private static final int[][] SHIFTS = new int[MAX_SUPPORTED_BITS_PER_VALUE+1][];
  static {
    for (int bpv : SUPPORTED_BITS_PER_VALUE) {
      initMasks(bpv);
    }
  }

  protected static void initMasks(int bpv) {
    int valuesPerBlock = Long.SIZE / bpv;
    long[] writeMasks = new long[valuesPerBlock];
    int[] shifts = new int[valuesPerBlock];
    long bits = (1L << bpv) - 1;
    for (int i = 0; i < valuesPerBlock; ++i) {
      shifts[i] = bpv * i;
      writeMasks[i] = ~(bits << shifts[i]);
    }
    WRITE_MASKS[bpv] = writeMasks;
    SHIFTS[bpv] = shifts;
  }

  public static Packed64SingleBlock create(int valueCount, int bitsPerValue) {
    if (isSupported(bitsPerValue)) {
      return new Packed64SingleBlock(valueCount, bitsPerValue);
    }
    throw new IllegalArgumentException("Unsupported bitsPerValue: " + bitsPerValue);
  }

  public static Packed64SingleBlock create(DataInput in,
      int valueCount, int bitsPerValue) throws IOException {
    Packed64SingleBlock reader = create(valueCount, bitsPerValue);
    for (int i = 0; i < reader.blocks.length; ++i) {
      reader.blocks[i] = in.readLong();
    }
    return reader;
  }

  public static boolean isSupported(int bitsPerValue) {
    return Arrays.binarySearch(SUPPORTED_BITS_PER_VALUE, bitsPerValue) >= 0;
  }

  public static float overheadPerValue(int bitsPerValue) {
    int valuesPerBlock = 64 / bitsPerValue;
    int overhead = 64 % bitsPerValue;
    return (float) overhead / valuesPerBlock;
  }

  final long[] blocks;
  final int valuesPerBlock;
  final int[] shifts;
  final long[] writeMasks;
  final long readMask;

  Packed64SingleBlock(int valueCount, int bitsPerValue) {
    super(valueCount, bitsPerValue);
    assert isSupported(bitsPerValue);
    valuesPerBlock = Long.SIZE / bitsPerValue;
    blocks = new long[requiredCapacity(valueCount, valuesPerBlock)];
    shifts = SHIFTS[bitsPerValue];
    writeMasks = WRITE_MASKS[bitsPerValue];
    readMask = ~writeMasks[0];
  }

  private static int requiredCapacity(int valueCount, int valuesPerBlock) {
    return valueCount / valuesPerBlock
        + (valueCount % valuesPerBlock == 0 ? 0 : 1);
  }

  private int blockOffset(int offset) {
    return offset / valuesPerBlock;
  }

  private int offsetInBlock(int offset) {
    return offset % valuesPerBlock;
  }

  @Override
  public long get(int index) {
    final int o = blockOffset(index);
    final int b = offsetInBlock(index);

    return (blocks[o] >>> shifts[b]) & readMask;
  }

  @Override
  public int get(int index, long[] arr, int off, int len) {
    assert len > 0 : "len must be > 0 (got " + len + ")";
    assert index >= 0 && index < valueCount;
    len = Math.min(len, valueCount - index);
    assert off + len <= arr.length;

    final int originalIndex = index;

    // go to the next block boundary
    final int offsetInBlock = offsetInBlock(index);
    if (offsetInBlock != 0) {
      for (int i = offsetInBlock; i < valuesPerBlock && len > 0; ++i) {
        arr[off++] = get(index++);
        --len;
      }
      if (len == 0) {
        return index - originalIndex;
      }
    }

    // bulk get
    assert offsetInBlock(index) == 0;
    final int startBlock = blockOffset(index);
    final int endBlock = blockOffset(index + len);
    final int diff = (endBlock - startBlock) * valuesPerBlock;
    index += diff; len -= diff;
    for (int block = startBlock; block < endBlock; ++block) {
      for (int i = 0; i < valuesPerBlock; ++i) {
        arr[off++] = (blocks[block] >> shifts[i]) & readMask;
      }
    }

    if (index > originalIndex) {
      // stay at the block boundary
      return index - originalIndex;
    } else {
      // no progress so far => already at a block boundary but no full block to
      // get
      assert index == originalIndex;
      return super.get(index, arr, off, len);
    }
  }

  @Override
  public void set(int index, long value) {
    final int o = blockOffset(index);
    final int b = offsetInBlock(index);

    blocks[o] = (blocks[o] & writeMasks[b]) | (value << shifts[b]);
  }

  @Override
  public int set(int index, long[] arr, int off, int len) {
    assert len > 0 : "len must be > 0 (got " + len + ")";
    assert index >= 0 && index < valueCount;
    len = Math.min(len, valueCount - index);
    assert off + len <= arr.length;

    final int originalIndex = index;

    // go to the next block boundary
    final int offsetInBlock = offsetInBlock(index);
    if (offsetInBlock != 0) {
      for (int i = offsetInBlock; i < valuesPerBlock && len > 0; ++i) {
        set(index++, arr[off++]);
        --len;
      }
      if (len == 0) {
        return index - originalIndex;
      }
    }

    // bulk set
    assert offsetInBlock(index) == 0;
    final int startBlock = blockOffset(index);
    final int endBlock = blockOffset(index + len);
    final int diff = (endBlock - startBlock) * valuesPerBlock;
    index += diff; len -= diff;
    for (int block = startBlock; block < endBlock; ++block) {
      long next = 0L;
      for (int i = 0; i < valuesPerBlock; ++i) {
        next |= (arr[off++] << shifts[i]);
      }
      blocks[block] = next;
    }

    if (index > originalIndex) {
      // stay at the block boundary
      return index - originalIndex;
    } else {
      // no progress so far => already at a block boundary but no full block to
      // set
      assert index == originalIndex;
      return super.set(index, arr, off, len);
    }
  }

  @Override
  public void fill(int fromIndex, int toIndex, long val) {
    assert fromIndex >= 0;
    assert fromIndex <= toIndex;
    assert (val & readMask) == val;

    if (toIndex - fromIndex <= valuesPerBlock << 1) {
      // there needs to be at least one full block to set for the block
      // approach to be worth trying
      super.fill(fromIndex, toIndex, val);
      return;
    }

    // set values naively until the next block start
    int fromOffsetInBlock = offsetInBlock(fromIndex);
    if (fromOffsetInBlock != 0) {
      for (int i = fromOffsetInBlock; i < valuesPerBlock; ++i) {
        set(fromIndex++, val);
      }
      assert offsetInBlock(fromIndex) == 0;
    }

    // bulk set of the inner blocks
    final int fromBlock = blockOffset(fromIndex);
    final int toBlock = blockOffset(toIndex);
    assert fromBlock * valuesPerBlock == fromIndex;

    long blockValue = 0L;
    for (int i = 0; i < valuesPerBlock; ++i) {
      blockValue = blockValue | (val << shifts[i]);
    }
    Arrays.fill(blocks, fromBlock, toBlock, blockValue);

    // fill the gap
    for (int i = valuesPerBlock * toBlock; i < toIndex; ++i) {
      set(i, val);
    }
  }

  @Override
  public void clear() {
    Arrays.fill(blocks, 0L);
  }

  public long ramBytesUsed() {
    return RamUsageEstimator.sizeOf(blocks);
  }

  @Override
  protected int getFormat() {
    return PackedInts.PACKED_SINGLE_BLOCK;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(bitsPerValue=" + bitsPerValue
        + ", size=" + size() + ", elements.length=" + blocks.length + ")";
  }

}