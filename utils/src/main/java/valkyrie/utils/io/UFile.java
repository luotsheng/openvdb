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

import valkyrie.utils.Assert;
import valkyrie.utils.Captor;
import valkyrie.utils.Optional;
import valkyrie.utils.collection.Lists;
import valkyrie.utils.exception.IOReadException;
import valkyrie.utils.string.StrStaticImports;
import valkyrie.utils.system.OS;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 扩展了 `java.io.File` 的自定义文件类，提供了增强的文件操作功能。
 * <p>
 * 该类在基本文件操作功能的基础上，增加了对文件路径的快速访问、文件扩展名的处理、文件强制删除等功能。
 * 它支持在 Windows 操作系统上对路径进行快速替换，提供了便捷的方法来获取和操作文件的扩展名。
 * <p>
 * 此外，本类还提供了对文件内容的随机读写操作的支持，包括读取不同类型的数据（如 int、long、float、double）。
 * 强制删除文件或目录的方法会递归删除目录下的所有内容，并尝试删除指定的文件或目录。
 * <p>
 * 本类支持的主要功能包括：
 * <ul>
 *     <li>路径快速访问</li>
 *     <li>文件和目录的强制删除</li>
 *     <li>文件扩展名的处理</li>
 *     <li>文件内容的随机读写访问</li>
 * </ul>
 * <p>
 * 需要注意的是，文件的随机读写访问功能要求在操作之前调用 `open()` 方法打开文件描述符。
 *
 * @see java.io.File
 * @since 1.0
 * @author Luo Tiansheng
 */
public class UFile extends java.io.File {

    /**
     * 空的 <code>File</code> 数组对象
     */
    private static final java.io.File[] EMPTY_FILE_ARRAY = new java.io.File[0];

    private static final String PATHNAME_DESKTOP_VARIABLE = "Desktop://";

    /**
     * 文件随机读写访问对象
     */
    private RandomAccessFile accessFile;

    /**
     * #brief: 快速访问指定路径
     *
     * <p>该函数用于在 Windows 操作系统上对指定路径进行快速替换操作。
     * 如果传入的路径以特定的桌面路径变量（{@code PATHNAME_DESKTOP_VARIABLE}）开头，
     * 将替换为实际的用户桌面路径。
     *
     * <p>注意事项：
     * - 该函数仅在 Windows 操作系统上执行替换操作，其他操作系统则直接返回原始路径。
     * - 确保 {@code PATHNAME_DESKTOP_VARIABLE} 已正确定义且匹配预期的路径格式。
     *
     * @param pathname
     *        要进行快速访问转换的路径字符串
     *
     * @return 如果是 Windows 操作系统且路径匹配桌面路径变量，返回替换后的路径；
     *         否则，返回原始路径
     */
    private static String quickAccessPath(String pathname) {
        if (OS.isWindows() || OS.isMacOS()) {
            /* Conversation Desktop */
            if (pathname.startsWith(PATHNAME_DESKTOP_VARIABLE))
                return pathname.replace(PATHNAME_DESKTOP_VARIABLE, OS.getUserHome("Desktop/"));
        }

        return pathname;
    }

    /**
     * Creates a new <code>File</code> instance by converting the given
     * pathname string into an abstract pathname.  If the given string is
     * the empty string, then the result is the empty abstract pathname.
     *
     * @param   pathname  A pathname string
     * @throws  NullPointerException
     *          If the <code>pathname</code> argument is <code>null</code>
     */
    public UFile(String pathname) {
        super(quickAccessPath(pathname));
    }

    /**
     * Creates a new <code>File</code> instance from a parent pathname string
     * and a child pathname string.
     *
     * <p> If <code>parent</code> is <code>null</code> then the new
     * <code>File</code> instance is created as if by invoking the
     * single-argument <code>File</code> constructor on the given
     * <code>child</code> pathname string.
     *
     * <p> Otherwise the <code>parent</code> pathname string is taken to denote
     * a directory, and the <code>child</code> pathname string is taken to
     * denote either a directory or a file.  If the <code>child</code> pathname
     * string is absolute then it is converted into a relative pathname in a
     * system-dependent way.  If <code>parent</code> is the empty string then
     * the new <code>File</code> instance is created by converting
     * <code>child</code> into an abstract pathname and resolving the result
     * against a system-dependent default directory.  Otherwise each pathname
     * string is converted into an abstract pathname and the child abstract
     * pathname is resolved against the parent.
     *
     * @param   parent  The parent pathname string
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If <code>child</code> is <code>null</code>
     */
    public UFile(String parent, String child) {
        super(parent, child);
    }

    /**
     * Creates a new <code>File</code> instance from a parent abstract
     * pathname and a child pathname string.
     *
     * <p> If <code>parent</code> is <code>null</code> then the new
     * <code>File</code> instance is created as if by invoking the
     * single-argument <code>File</code> constructor on the given
     * <code>child</code> pathname string.
     *
     * <p> Otherwise the <code>parent</code> abstract pathname is taken to
     * denote a directory, and the <code>child</code> pathname string is taken
     * to denote either a directory or a file.  If the <code>child</code>
     * pathname string is absolute then it is converted into a relative
     * pathname in a system-dependent way.  If <code>parent</code> is the empty
     * abstract pathname then the new <code>File</code> instance is created by
     * converting <code>child</code> into an abstract pathname and resolving
     * the result against a system-dependent default directory.  Otherwise each
     * pathname string is converted into an abstract pathname and the child
     * abstract pathname is resolved against the parent.
     *
     * @param   parent  The parent abstract pathname
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If <code>child</code> is <code>null</code>
     */
    public UFile(java.io.File parent, String child) {
        super(parent, child);
    }

    /**
     * Creates a new <tt>File</tt> instance by converting the given
     * <tt>file:</tt> URI into an abstract pathname.
     *
     * <p> The exact form of a <tt>file:</tt> URI is system-dependent, hence
     * the transformation performed by this constructor is also
     * system-dependent.
     *
     * <p> For a given abstract pathname <i>f</i> it is guaranteed that
     *
     * <blockquote><tt>
     * new File(</tt><i>&nbsp;f</i><tt>.{@link #toURI() toURI}()).equals(</tt><i>&nbsp;f</i><tt>.{@link #getAbsoluteFile() getAbsoluteFile}())
     * </tt></blockquote>
     *
     * so long as the original abstract pathname, the URI, and the new abstract
     * pathname are all created in (possibly different invocations of) the same
     * Java virtual machine.  This relationship typically does not hold,
     * however, when a <tt>file:</tt> URI that is created in a virtual machine
     * on one operating system is converted into an abstract pathname in a
     * virtual machine on a different operating system.
     *
     * @param  uri
     *         An absolute, hierarchical URI with a scheme equal to
     *         <tt>"file"</tt>, a non-empty path component, and undefined
     *         authority, query, and fragment components
     *
     * @throws  NullPointerException
     *          If <tt>uri</tt> is <tt>null</tt>
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on the parameter do not hold
     *
     * @see #toURI()
     * @see URI
     * @since 1.4
     */
    public UFile(URI uri) {
        super(uri);
    }

    /**
     * 通过一个 {@link java.io.File} 对象来构建一个 {@link UFile} 对象实例，并且参数 File
     * 不能是空的。
     *
     * @param file java.io 下的 File 对象实例
     */
    public UFile(java.io.File file) {
        this(file.getPath());
    }

    /**
     * 将指定的路径名字符串包装为自定义的 `File` 对象。
     * <p>
     * 该方法接收一个表示文件路径的字符串，并将其包装为当前定义的自定义 `File` 对象，以便进一步处理。
     *
     * @param pathname 要包装的文件路径名字符串。
     * @return 一个新的 `File` 对象，包装了传入的文件路径名字符串。
     */
    public static UFile wrap(String pathname) {
        return new UFile(pathname);
    }

    /**
     * 将指定的 `java.io.File` 对象包装为自定义的 `File` 对象。
     * <p>
     * 该方法接收一个标准的 `java.io.File` 对象，并将其包装为当前定义的自定义 `File` 对象，以便进一步处理。
     *
     * @param file 要包装的 `java.io.File` 对象。
     * @return 一个新的 `File` 对象，包装了传入的 `java.io.File` 对象。
     */
    public static UFile wrap(java.io.File file) {
        return new UFile(file);
    }

    /**
     * Returns the abstract pathname of this abstract pathname's parent,
     * or {@code null} if this pathname does not name a parent
     * directory.
     *
     * <p> The <em>parent</em> of an abstract pathname consists of the
     * pathname's prefix, if any, and each name in the pathname's name
     * sequence except for the last.  If the name sequence is empty then
     * the pathname does not name a parent directory.
     *
     * @return  The abstract pathname of the parent directory named by this
     *          abstract pathname, or {@code null} if this pathname
     *          does not name a parent
     *
     * @since 1.2
     */
    @Override
    public UFile getParentFile() {
        java.io.File parentFile = super.getParentFile();
        return parentFile != null ? wrap(parentFile) : null;
    }

    /**
     * Returns an array of abstract pathnames denoting the files in the
     * directory denoted by this abstract pathname.
     *
     * <p> If this abstract pathname does not denote a directory, then this
     * method returns {@code null}.  Otherwise an array of {@code File} objects
     * is returned, one for each file or directory in the directory.  Pathnames
     * denoting the directory itself and the directory's parent directory are
     * not included in the result.  Each resulting abstract pathname is
     * constructed from this abstract pathname using the {@link #UFile(java.io.File,
     * String) File(File,&nbsp;String)} constructor.  Therefore if this
     * pathname is absolute then each resulting pathname is absolute; if this
     * pathname is relative then each resulting pathname will be relative to
     * the same directory.
     *
     * <p> There is no guarantee that the name strings in the resulting array
     * will appear in any specific order; they are not, in particular,
     * guaranteed to appear in alphabetical order.
     *
     * <p> Note that the {@link java.nio.file.Files} class defines the {@link
     * java.nio.file.Files#newDirectoryStream(Path) newDirectoryStream} method
     * to open a directory and iterate over the names of the files in the
     * directory. This may use less resources when working with very large
     * directories.
     *
     * @return  An array of abstract pathnames denoting the files and
     *          directories in the directory denoted by this abstract pathname.
     *          The array will be empty if the directory is empty.  Returns
     *          {@code null} if this abstract pathname does not denote a
     *          directory, or if an I/O error occurs.
     *
     * @since  1.2
     */
    @Override
    public UFile[] listFiles() {
        File[] files = super.listFiles();
        if (files == null)
            return new UFile[0];
        List<UFile> list = Lists.map(files, UFile::wrap);
        UFile[] fs = new UFile[list.size()];
        list.toArray(fs);
        return fs;
    }

    /**
     * @return 返回一个干净的文件名称，指不带扩展后缀的
     *         文件名
     */
    public String getCleanName() {
        String name = getName();
        return StrStaticImports.strcut(getName(), 0, name.lastIndexOf("."));
    }

    /**
     * 比较两个文件的扩展类型是否一致。如果扩展名一致的话则返回 `true`。文件
     * 的扩展名需要带 '.'，如：.pdf
     *
     * @param extension 文件扩展名
     * @return 当前 File 文件和 {@code extension} 一致返回 `true`
     */
    public boolean typeEquals(String extension) {
        return StrStaticImports.streq(extension, getExtension());
    }

    /**
     * #brief: 检查文件扩展名是否匹配
     *
     * 该函数用于检查当前对象的文件扩展名是否匹配指定的扩展名列表。
     *
     * @param extensions
     *        需要匹配的文件扩展名列表，可变参数形式
     *
     * @return 如果当前对象的扩展名与任一指定的扩展名匹配，返回 {@code true}；
     *         否则返回 {@code false}
     */
    public boolean typeMatch(String... extensions) {
        return StrStaticImports.strhas(getExtension(), extensions);
    }

    /**
     * @return 返回文件扩展名（后缀）
     */
    public String getExtension() {
        String name = getName();
        int index = name.indexOf(".");
        if (index == -1)
            return "";
        return StrStaticImports.strcut(getName(), index, 0);
    }

    private boolean forceDeleteDirectory(UFile dir) {
        List<UFile> children = Lists.map(dir.listFiles(), UFile::new);
        for (UFile child : children) {
            if (child.isFile()) {
                child.forceDeleteFile();
            } else {
                child.forceDeleteDirectory();
            }
        }
        return dir.forceDeleteFile();
    }

    private boolean forceDeleteDirectory() {
        return forceDeleteDirectory(this);
    }

    private boolean forceDeleteFile() {
        boolean retval;
        if (!(retval = delete())) {
            System.gc();
            retval = delete();
        }
        return retval;
    }

    /**
     * 强制删除一个文件或目录，文件或目录必须存在，否则将什么都不做。如果是目录
     * 会先递归删除目录下的所有文件，然后再删除目录本身。
     *
     * @return 是否删除成功
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean forceDelete() {
        if (exists())
            return isFile() ? forceDeleteFile() : forceDeleteDirectory();
        return false;
    }

    /**
     * 检查文件是否存在，如果不存在就创建一个，但前提是 {@code enableAutoCreate}
     * 参数必须是 {@code true}。
     */
    private void checkFile(boolean autoCreate) {
        if (autoCreate && !exists())
            Captor.call(this::createNewFile);
        Assert.isTrue(exists(), "%s （系统找不到指定的文件，文件不存在）", getPath());
    }

    /**
     * 打开一个文件输入流，通过参数 {@code autoCreate} 检查当文件不存在时是否
     * 自动创建一个新的文件。<p>
     *
     * 如果 {@code autoCreate} 为 {@code false} 时，当文件不存在就会抛出一个
     * 运行时断言异常。
     *
     * @param autoCreate 文件不存在时是否创建
     * @return 打开描述符后的文件输入流对象
     */
    private FileByteReader openByteReader(boolean autoCreate) {
        checkFile(autoCreate);
        return Captor.call(() -> new FileByteReader(this));
    }

    /**
     * 打开一个文件输出流，通过参数 {@code autoCreate} 检查当文件不存在时是否
     * 自动创建一个新的文件。<p>
     *
     * 如果 {@code autoCreate} 为 {@code false} 时，当文件不存在就会抛出一个
     * 运行时断言异常。
     *
     * @param autoCreate 文件不存在时是否创建
     * @return 打开描述符后的文件输出流对象
     */
    private FileByteWriter openByteWriter(boolean autoCreate) {
        checkFile(autoCreate);
        return Captor.call(() -> new FileByteWriter(this));
    }

    /**
     * 打开文件输入流，如果文件不存在则会自动创建一个新的空文件。避免当文件
     * 不存在时系统抛出 FileNotFound 异常。
     *
     * @return 打开描述符后的文件输入流对象
     */
    public FileByteReader openByteReader() {
        return openByteReader(true);
    }

    /**
     * 打开文件输出流，如果文件不存在则会自动创建一个新的空文件。避免当文件
     * 不存在时系统抛出 FileNotFound 异常。
     *
     * @return 打开描述符后的文件输入流对象
     */
    public FileByteWriter openByteWriter() {
        return openByteWriter(true);
    }

    /**
     * 打开文件输入流，如果当文件不存在时则会抛出一个 FileNotFound 异常，该函数
     * 不会自动创建文件。
     *
     * @return 打开描述符后的文件输入流对象
     */
    public FileByteReader openByteReaderDisabled() {
        return openByteReader(false);
    }

    /**
     * 打开文件输出流，如果当文件不存在时则会抛出一个 FileNotFound 异常，该函数
     * 不会自动创建文件。
     *
     * @return 打开描述符后的文件输出流对象
     */
    public FileByteWriter openByteWriterDisabled() {
        return openByteWriter(false);
    }

    /**
     * 将整个文件以字符串的形式读取到内存中，并将字符串对象引用返回
     * 出去。
     *
     * @return 文件内容
     */
    public String strread() {
        return IOUtils.strread(openByteReader());
    }

    /**
     * #brief: 读取并返回文件的所有字节内容
     *
     * <p>该方法打开文件，读取文件的所有内容并将其存储到字节数组中，最后关闭文件。
     * 返回包含文件所有字节数据的数组。
     *
     * <p>方法会先根据文件长度初始化一个字节数组，然后将文件内容读取到该数组中。
     * 调用完毕后，会自动关闭文件。
     *
     * @return 包含文件内容的字节数组
     */
    public byte[] readBytes() {
        open();
        byte[] b = new byte[(int) length()];
        read(b);
        close();
        return b;
    }

    //////////////////////////////////////////////////////////////////////////////
    /// RandomAccessFile
    //////////////////////////////////////////////////////////////////////////////

    /**
     * 打开文件描述符，使得 <code>File</code> 文件对象支持系统随机读写
     * 访问。随机读写机制能够更灵活的操作文件内容，并写入。性能更高。
     * 同时当文件打开成功以后，当前对象将支持所有随机读写访问函数。
     * <p>
     *
     * 当调用 <code>open()</code> 函数时，操作系统会打开一个文件描述符，
     * 该描述符由操作系统中的文件系统管理，
     * <p>
     *
     * 默认同时支持读写操作。
     *
     * @return 描述符是否打开成功
     */
    public boolean open() {
        return open("rwd");
    }

    /**
     * 打开文件描述符，使得 <code>File</code> 文件对象支持系统随机读写
     * 访问。随机读写机制能够更灵活的操作文件内容，并写入。性能更高。
     * 同时当文件打开成功以后，当前对象将支持所有随机读写访问函数。
     *
     * <p>当调用 <code>open()</code> 函数时，操作系统会打开一个文件描述符，
     * 该描述符由操作系统中的文件系统管理，
     *
     * @param mode 打开模式，r 表示支持读取，w 表示支持写入，也可以
     *             连起来写，rw 表示同时支持读取和写入
     *
     * @return 描述符是否打开成功
     */
    public boolean open(String mode) {
        return (accessFile = Optional.ifError(() -> new RandomAccessFile(this, mode), null)) != null;
    }

    /**
     * 检查文件描述符是否打开
     */
    private void checkOpen() {
        Assert.isTrue(accessFile != null, "Please call open() before random access methods execute.");
    }

    /**
     * Attempts to skip over {@code n} bytes of input discarding the
     * skipped bytes.
     * <p>
     *
     * This method may skip over some smaller number of bytes, possibly zero.
     * This may result from any of a number of conditions; reaching end of
     * file before {@code n} bytes have been skipped is only one
     * possibility. This method never throws an {@code EOFException}.
     * The actual number of bytes skipped is returned.  If {@code n}
     * is negative, no bytes are skipped.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     */
    public int skipBytes(int n) {
        checkOpen();
        return Captor.call(() -> accessFile.skipBytes(n));
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs.  The offset may be
     * set beyond the end of the file. Setting the offset beyond the end
     * of the file does not change the file length.  The file length will
     * change only by writing after the offset has been set beyond the end
     * of the file.
     *
     * @param      pos   the offset position, measured in bytes from the
     *                   beginning of the file, at which to set the file
     *                   pointer.
     */
    public void seek(long pos) {
        checkOpen();
        Captor.call(() -> accessFile.seek(pos));
    }

    /**
     * 从当前文件数据中读取一个字节并返回，返回的字节类型时一个 int
     * 类型，int 的取值范围是从 0 - 255，<code>0x00-0x0ff</code>，
     * 这个方法会阻塞执行。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @return 返回一个取值范围从 0-255 的字节数据，如果返回
     *         值等于 <code>-1</code> 那么表示文件已读取到
     *         末尾。
     */
    public int read() {
        checkOpen();
        return Captor.call(() -> accessFile.read());
    }

    /**
     * 从当前文件的数据中读取 <code>b.length</code> 字节到 <code>b</code> 字节
     * 缓冲区数组中，这个方法会阻塞执行。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param b   字节缓冲区数组
     */
    public void read(byte[] b) {
        checkOpen();
        Captor.call(() -> accessFile.read(b));
    }

    /**
     * 从当前文件的数据中读取 <code>len</code> 字节到 <code>b</code> 字节
     * 缓冲区数组中，这个方法会阻塞执行。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param b   字节缓冲区数组
     * @param off 缓冲区偏移量，表示从 <code>b</code> 缓冲区的 <code>off</code>
     *            位置开始写入。
     * @param len 数据读取长度，表示要从文件中读取 <code>len</code> 个字节
     *            数据到字节缓冲区中。
     */
    public void read(byte[] b, int off, int len) {
        checkOpen();
        Captor.call(() -> accessFile.read(b, off, len));
    }

    /**
     * 从当前访问偏移量的位置在文件数据中往后读取 {@link Integer#BYTES} 个
     * 字节数据，并将读取到的字节数据转换成一个 int 类型的数据。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @return 返回从文件中读取到的 int 数据
     */
    public int readInt() {
        checkOpen();
        return Captor.call(() -> accessFile.readInt());
    }

    /**
     * 从当前访问偏移量的位置在文件数据中往后读取 {@link Long#BYTES} 个
     * 字节数据，并将读取到的字节数据转换成一个 long 类型的数据类。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @return 返回从文件中读取到的 long 数据
     */
    public long readLong() {
        checkOpen();
        return Captor.call(() -> accessFile.readLong());
    }

    /**
     * 从当前访问偏移量的位置在文件数据中往后读取 {@link Float#BYTES} 个
     * 字节数据，并将读取到的字节数据转换成一个 float 类型的数据类。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @return 返回从文件中读取到的 float 数据
     */
    public float readFloat() {
        checkOpen();
        return Captor.call(() -> accessFile.readFloat());
    }

    /**
     * 从当前访问偏移量的位置在文件数据中往后读取 {@link Double#BYTES} 个
     * 字节数据，并将读取到的字节数据转换成一个 double 类型的数据类。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @return 返回从文件中读取到的 double 数据
     */
    public double readDouble() {
        checkOpen();
        return Captor.call(() -> accessFile.readDouble());
    }

    /**
     * 写入一个字节数据到当前的文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param b 需要写入的字节数据
     */
    public void write(byte b) {
        checkOpen();
        Captor.call(() -> accessFile.write(b));
    }

    /**
     * 写入一个字节数组数据到当前的文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param b 需要写入的字节数组
     */
    public void write(byte[] b) {
        checkOpen();
        Captor.call(() -> accessFile.write(b));
    }

    /**
     * 根据提供的偏移量和长度写入将字节数组中的数据写入到当前
     * 文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param b   字节缓冲区数组
     * @param off 缓冲区偏移量，表示从 <code>b</code> 缓冲区的 <code>off</code>
     *            位置开始写入。
     * @param len 数据写入长度，表示要从字节缓冲区中读取 <code>len</code> 个字节
     *            数据文件中。
     */
    public void write(byte[] b, int off, int len) {
        checkOpen();
        Captor.call(() -> accessFile.write(b, off, len));
    }

    /**
     * 写入一个 Integer 数据到文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param value 需要写入的 Integer 数据。
     */
    public void writeInt(int value) {
        checkOpen();
        Captor.call(() -> accessFile.writeInt(value));
    }

    /**
     * 写入一个 Long 数据到文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param value 需要写入的 Long 数据。
     */
    public void writeLong(long value) {
        checkOpen();
        Captor.call(() -> accessFile.writeLong(value));
    }

    /**
     * 写入一个 Float 数据到文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param value 需要写入的 Float 数据。
     */
    public void writeFloat(float value) {
        checkOpen();
        Captor.call(() -> accessFile.writeFloat(value));
    }

    /**
     * 写入一个 Double 数据到文件中。
     * <p>
     * 使用随机读写函数前，请务必先打开文件随机读写访问对象，使用
     * <code>open()</code> 函数打开随机读写访问对象，否则禁止使用
     * 随机读写函数。
     *
     * @param value 需要写入的 Double 数据。
     */
    public void writeDouble(double value) {
        checkOpen();
        Captor.call(() -> accessFile.writeDouble(value));
    }

    /**
     * 关闭文件描述符，当文件描述符被关闭后，<code>File</code> 对象将
     * 不再支持随机读写访问，并释放出文件描述符句柄。如果需要重新使用
     * 随机读写访问功能，重新打开文件描述符即可，
     */
    public void close() {
        checkOpen();
        IOUtils.closeQuietly(accessFile);
        accessFile = null;
    }

    /**
     * 加载配置文件的属性。
     * <p>
     * 该方法从指定的字节读取器中加载属性文件，并返回包含所有属性键值对的 `Properties` 对象。
     * 加载过程中，使用 `Capturer.call()` 方法来捕获可能的异常。
     *
     * @return 加载的 `Properties` 对象，包含从配置文件中读取的所有键值对。
     */
    public Properties loadProperties() {
        Properties properties = new Properties();
        try(FileByteReader fileByteReader = openByteReaderDisabled()) {
            properties.load(fileByteReader);
        } catch (Exception e) {
            throw new IOReadException(e);
        }
        return properties;
    }

    private BasicFileAttributes getBasicFileAttributes() throws IOException
    {
        return Files.readAttributes(Paths.get(getPath()), BasicFileAttributes.class);
    }

    public String getOwner()
    {
        return Optional.ifError(() -> Files.getOwner(toPath()).getName(), null);
    }

    public Date getCreatingTime() {
        return Optional.ifError(() -> {
            FileTime fileTime = getBasicFileAttributes().creationTime();
            return new Date(fileTime.toMillis());
        }, null);
    }

    public Date getLastModifiedTime() {
        return Optional.ifError(() -> {
            FileTime fileTime = getBasicFileAttributes().lastModifiedTime();
            return new Date(fileTime.toMillis());
        }, null);
    }

    public Date getLastAccessTime() {
        return Optional.ifError(() -> {
            FileTime fileTime = getBasicFileAttributes().lastAccessTime();
            return new Date(fileTime.toMillis());
        }, null);
    }

}
