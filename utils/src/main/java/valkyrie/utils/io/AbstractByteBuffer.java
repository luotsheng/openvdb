package valkyrie.utils.io;

/* -------------------------------------------------------------------------------- *\
|*                                                                                  *|
|*    Copyright (C) 2019-2024 Luo Tiansheng All rights reserved.                    *|
|*                                                                                  *|
|*    Licensed under the Apache License, Version 2.0 (the "License");               *|
|*    you may not use this file except in compliance with the License.              *|
|*    You may obtain a copy of the License at                                       *|
|*                                                                                  *|
|*        http://www.apache.org/licenses/LICENSE-2.0                                *|
|*                                                                                  *|
|*    Unless required by applicable law or agreed to in writing, software           *|
|*    distributed under the License is distributed on an "AS IS" BASIS,             *|
|*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.      *|
|*    See the License for the specific language governing permissions and           *|
|*    limitations under the License.                                                *|
|*                                                                                  *|
\* -------------------------------------------------------------------------------- */

/* Creates on 2023/5/8. */

import valkyrie.utils.ArrayUtils;
import valkyrie.utils.string.StrStaticImports;

/**
 * ByteBuffer 默认抽象实现类
 *
 * @author Luo Tiansheng
 */
public abstract class AbstractByteBuffer extends ByteBuffer {

    /** 读写指针 */
    protected int index;

    /** 字节缓冲区内部真实数据大小 */
    protected int capacity;

    /** 标记当前索引位置 */
    protected int markIndex = 0;

    /* 临时缓冲区 */
    private final byte[] tmp = new byte[16];

    @Override
    public ByteBuffer duplicate() {
        return wrap(toByteArray());
    }

    public int readableBytes() {
        return capacity - index;
    }

    @Override
    public int writeableBytes() {
        return size() - index;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    public ByteBuffer seekSet(int off) {
        index = off;
        return this;
    }

    public ByteBuffer seekCur(int off) {
        index += off;
        return this;
    }

    public ByteBuffer seekEnd(int off) {
        index = capacity - off;
        return this;
    }

    @Override
    public void skipBytes(int len) {
        seekCur(len);
    }

    @Override
    public ByteBuffer markIndex() {
        markIndex = index;
        return this;
    }

    @Override
    public ByteBuffer reset() {
        index = markIndex;
        return this;
    }

    @Override
    public ByteBuffer rewind() {
        index = 0;
        return this;
    }

    public byte readByte() {
        readBytes(tmp, 0, 1);
        return tmp[0];
    }

    @Override
    public char readChar() {
        readBytes(tmp, 0, Character.BYTES);
        return (char) ((tmp[0] << 8) | (tmp[1] & 0xFF));
    }

    @Override
    public char[] readChars(int n) {
        char[] c = new char[n];
        for (int i = 0; i < n; i++)
            c[i] = readChar();
        return c;
    }

    public short readShort() {
        readBytes(tmp, 0, Short.BYTES);
        return (short) ((tmp[0] << 8) | tmp[1] & 0xFF);
    }

    public int readInt() {
        readBytes(tmp, 0, Integer.BYTES);
        return ((tmp[0] & 0xFF) << 24)
                | ((tmp[1] & 0xFF) << 16)
                | ((tmp[2] & 0xFF) << 8)
                |  (tmp[3] & 0xFF);
    }

    public long readLong() {
        readBytes(tmp, 0, Long.BYTES);
        long value = 0L;
        for (int i = 0; i < Long.BYTES; i++)
            value = (value << 8) | (tmp[i] & 0xFF);
        return value;
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public byte[] readBytes(int nb) {
        byte[] buf = new byte[nb];
        readBytes(buf, 0, nb);
        return buf;
    }

    public int readBytes(byte[] b, int off, int len) {
        ArrayUtils.checkIndexSize(off, len, b.length);
        int remcap = capacity - index;
        if (remcap == 0)
            return IOUtils.EOF;
        if (len > remcap)
            len = remcap;
        read0(b, off, len);
        return len;
    }

    public ByteBuffer writeByte(byte b) {
        tmp[0] = b;
        return writeBytes(tmp, 0, Byte.BYTES);
    }

    @Override
    public ByteBuffer writeChar(char c) {
        return writeBytes(new byte[] {
                (byte) (c >> 8),
                (byte)  c,
        });
    }

    @Override
    public ByteBuffer writeChars(char[] ch) {
        for (char c : ch)
            writeChar(c);
        return this;
    }

    public ByteBuffer writeShort(short v) {
        return writeBytes(new byte[]{
                (byte) (v >> 8),
                (byte)  v,
        }, 0, Short.BYTES);
    }

    public ByteBuffer writeInt(int i) {
        tmp[0] = (byte) ((i >> 24) & 0xFF);
        tmp[1] = (byte) ((i >> 16) & 0xFF);
        tmp[2] = (byte) ((i >> 8) & 0xFF);
        tmp[3] = (byte) (i & 0xFF);
        return writeBytes(tmp, 0, Integer.BYTES);
    }

    public ByteBuffer writeLong(long l) {
        for (int i = 0; i < Long.BYTES; i++)
            tmp[i] = (byte) ((l >> ((Long.BYTES - i - 1) * 8)) & 0xFF);
        return writeBytes(tmp, 0, Long.BYTES);
    }

    @Override
    public ByteBuffer writeFloat(float f) {
        return writeInt(Float.floatToIntBits(f));
    }

    @Override
    public ByteBuffer writeDouble(double d) {
        return writeLong(Double.doubleToLongBits(d));
    }

    public ByteBuffer writeBytes(byte[] b) {
        return writeBytes(b, 0, b.length);
    }

    public ByteBuffer writeBytes(byte[] b, int off, int len) {
        ArrayUtils.checkIndexSize(off, len, b.length);
        write0(b, off, len);
        return this;
    }

    abstract void read0(byte[] b, int off, int len);

    abstract void write0(byte[] b, int off, int len);

    public byte[] toByteArray() {
        byte[] retval = new byte[capacity];
        markIndex();
        seekSet(0);
        readBytes(retval, 0, retval.length);
        reset();
        return retval;
    }

    @Override
    public String toString() {
        return StrStaticImports.fmt("%s [size=%s, cap=%s, index=%s]", super.toString(), size(), capacity(), index());
    }
}
