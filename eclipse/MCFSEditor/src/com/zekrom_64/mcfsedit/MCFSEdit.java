package com.zekrom_64.mcfsedit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class MCFSEdit extends JFrame implements ActionListener {
	
	private static byte[] bootloader;
	private static byte[] mcfsMagic = new byte[] {
		(byte)0x4D, (byte)0x43, (byte)0x46, (byte)0x53	
	};
	
	private class MCFSFile {
		
		public int ptr;
		public short startSector;
		public short sectorSize;
		public boolean bootFile;
		public String fileName;
		
		@Override
		public String toString() {
			return fileName;
		}
		
	}
	
	private DefaultListModel<MCFSFile> files = new DefaultListModel<>();
	private boolean modified = false;
	private File currentFile = null;
	private byte[] disk = new byte[128*2048];

	private JPanel contentPane;
	private JList<MCFSFile> lstFiles;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			// Bootloader is <512 bytes but use variable-size arrays in case of changes
			InputStream in = MCFSEdit.class.getResourceAsStream("mcfs_boot.bin");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[512];
			int read = 0;
			while((read = in.read(buffer)) != -1) baos.write(buffer, 0, read);
			bootloader = baos.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		EventQueue.invokeLater(() -> {
			try {
				MCFSEdit frame = new MCFSEdit();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	private void save(boolean saveAs) {
		File saveFile = currentFile;
		if (saveAs || saveFile == null) {
			JFileChooser jfc = new JFileChooser();
			jfc.setCurrentDirectory(new File("."));
			switch(jfc.showSaveDialog(null)) {
			case JFileChooser.APPROVE_OPTION:
				break;
			default:
				return;
			}
			saveFile = jfc.getSelectedFile();
			currentFile = saveFile;
		}
		try(FileOutputStream fos = new FileOutputStream(saveFile)) {
			fos.write(disk);
			modified = false;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean saveModified() {
		if (modified) {
			int opt = JOptionPane.showConfirmDialog(null, "Disk image has unsaved changes, save?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
			switch(opt) {
			case JOptionPane.YES_OPTION:
				break;
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;
			}
			save(false);
			return true;
		}
		return true;
	}
	
	private static int blend(int i1, int i2) {
		return (i1 * i2) / 255;
	}
	
	private static Color blend(Color c1, Color c2) {
		return new Color(
				blend(c1.getRed(), c2.getRed()),
				blend(c1.getGreen(), c2.getGreen()),
				blend(c1.getBlue(), c2.getBlue())
		);
	}

	/**
	 * Create the frame.
	 */
	public MCFSEdit() {
		setTitle("MCFSEdit");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				MCFSEdit.this.actionPerformed(new ActionEvent(arg0.getSource(), arg0.getID(), "quit"));
			}
			
		});
		setBounds(100, 100, 250, 300);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mntmNew.setActionCommand("new");
		mntmNew.addActionListener(this);
		mntmNew.setToolTipText("Creates a new empty MCFS volume");
		mnFile.add(mntmNew);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mntmOpen.setActionCommand("open");
		mntmOpen.addActionListener(this);
		mntmOpen.setToolTipText("Open an existing MCFS volume");
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.setActionCommand("save");
		mntmSave.addActionListener(this);
		mntmSave.setToolTipText("Save the current volume");
		mnFile.add(mntmSave);
		
		JMenuItem mntmSaveas = new JMenuItem("Save-As");
		mntmSaveas.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
		mntmSaveas.setActionCommand("saveas");
		mntmSaveas.addActionListener(this);
		mntmSaveas.setToolTipText("Save the current volume to a specific file");
		mnFile.add(mntmSaveas);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK));
		mntmClose.setActionCommand("close");
		mntmClose.addActionListener(this);
		mntmClose.setToolTipText("Close the current volume");
		mnFile.add(mntmClose);
		
		JMenuItem mntmQuit = new JMenuItem("Quit");
		mntmQuit.setActionCommand("quit");
		mntmQuit.addActionListener(this);
		mntmQuit.setToolTipText("Quit the volume editor");
		mnFile.add(mntmQuit);
		
		JMenu mnDisk = new JMenu("Disk");
		menuBar.add(mnDisk);
		
		JMenuItem mntmImport = new JMenuItem("Import");
		mntmImport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mntmImport.setActionCommand("import");
		mntmImport.addActionListener(this);
		mntmImport.setToolTipText("Imports a file to the MCFS volume");
		mnDisk.add(mntmImport);
		
		JMenuItem mntmExport = new JMenuItem("Export");
		mntmExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mntmExport.setActionCommand("export");
		mntmExport.addActionListener(this);
		mntmExport.setToolTipText("Exports a file from the MCFS volume");
		mnDisk.add(mntmExport);
		
		JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		mntmDelete.setActionCommand("delete");
		mntmDelete.addActionListener(this);
		mntmDelete.setToolTipText("Deletes a file from the MCFS volume");
		mnDisk.add(mntmDelete);
		
		JMenuItem mntmValidate = new JMenuItem("Validate");
		mntmValidate.setActionCommand("validate");
		mntmValidate.addActionListener(this);
		
		JMenuItem mntmToggleBootFile = new JMenuItem("Toggle Boot File");
		mntmToggleBootFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
		mntmToggleBootFile.setActionCommand("toggleboot");
		mntmToggleBootFile.addActionListener(this);
		mnDisk.add(mntmToggleBootFile);
		mntmValidate.setToolTipText("[WIP] Validates if the data on the volume is correctly formatted");
		mnDisk.add(mntmValidate);
		
		JMenuItem mntmOrganize = new JMenuItem("Organize");
		mntmOrganize.setActionCommand("organize");
		mntmOrganize.addActionListener(this);
		mntmOrganize.setToolTipText("[WIP] Organizes the files on the volume such that all the sectors are contiguous");
		mnDisk.add(mntmOrganize);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		lstFiles = new JList<MCFSFile>(files);
		lstFiles.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (((MCFSFile)value).bootFile) this.setBackground(blend(this.getBackground(), Color.YELLOW));
				return this;
			}
			
		});
		scrollPane.setViewportView(lstFiles);
		
		lstFiles.setDragEnabled(true);
		lstFiles.setDropMode(DropMode.USE_SELECTION);
		lstFiles.setTransferHandler(new TransferHandler() {

			@Override
			public boolean canImport(TransferSupport support) {
				for(DataFlavor flavor : support.getDataFlavors())
					if (flavor.isFlavorJavaFileListType()) return true;
				return false;
			}

			@Override
			public boolean importData(TransferSupport support) {
				try {
					@SuppressWarnings("unchecked")
					List<File> files = (List<File>)support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					File file = files.get(0);
					if (currentFile == null) {
						// Close any current file
						if (!saveModified()) return false;
						close();
						try(FileInputStream fis = new FileInputStream(file)) {
							open(fis);
						}
						currentFile = file;
					} else {
						String name = JOptionPane.showInputDialog(null, "Enter name of file on disk");
						try(FileInputStream fis = new FileInputStream(file)) {
							import0(fis, name);
						}
						reloadFiles();
						modified = true;
					}
				} catch(Exception e) {
					JOptionPane.showMessageDialog(null, "Exception opening file: " + e, "Error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					return false;
				}
				return true;
			}
			
		});
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		switch(arg0.getActionCommand()) {
		case "new": {
			saveModified();
			files.clear();
			String name = JOptionPane.showInputDialog(null, "Enter new volume name:");
			// Reset the disk
			Arrays.fill(disk, (byte)0);
			// Copy magic
			System.arraycopy(mcfsMagic, 0, disk, 124, 4);
			// Copy bootloader
			System.arraycopy(bootloader, 0, disk, 0, bootloader.length);
			// Set disk name
			byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
			if (nameBytes.length > 28) nameBytes = Arrays.copyOf(nameBytes, 28);
			this.setTitle("MCFSEdit - " + new String(nameBytes, StandardCharsets.US_ASCII));
			nameBytes[nameBytes.length-1] |= 0x80;
			System.arraycopy(nameBytes, 0, disk, 4 + 6*128, nameBytes.length);
			// Set initial SAM bits
			disk[4*128] = (byte)0xFF;
			disk[1+4*128] = (byte)0xFF;
			// Set as modified
			modified = true;
			currentFile = null;
		} break;
		case "open": {
			// Save any changes to the current file
			if (!saveModified()) return;
			// Select file
			JFileChooser jfc = new JFileChooser();
			if (currentFile != null) jfc.setCurrentDirectory(currentFile.getParentFile());
			else jfc.setCurrentDirectory(new File("."));
			if (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
			// Close file
			close();
			// Attempt to read file
			try (FileInputStream fin = new FileInputStream(jfc.getSelectedFile())) {
				open(fin);
				currentFile = jfc.getSelectedFile();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Exception opening file: " + e, "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		} break;
		case "save":
			save(false);
			break;
		case "saveas":
			save(true);
			break;
		case "close":
			if (!saveModified()) return;
			close();
			break;
		case "quit":
			if (!saveModified()) return;
			MCFSEdit.this.dispose();
			break;
		case "import": {
			JFileChooser jfc = new JFileChooser();
			if (currentFile != null) jfc.setCurrentDirectory(currentFile.getParentFile());
			if (jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
			try (FileInputStream fis = new FileInputStream(jfc.getSelectedFile())) {
				String name = JOptionPane.showInputDialog(null, "Enter name of file on disk");
				import0(fis, name);
			} catch (Exception e) {
				e.printStackTrace();
			}
			modified = true;
			reloadFiles();
		}
		case "export": {
			MCFSFile file = lstFiles.getSelectedValue();
			if (file != null) {
				JFileChooser jfc = new JFileChooser();
				if (currentFile != null) jfc.setCurrentDirectory(currentFile.getParentFile());
				else jfc.setCurrentDirectory(new File("."));
				int opt = jfc.showSaveDialog(null);
				if (opt != JFileChooser.APPROVE_OPTION) return;
				try(FileOutputStream fos = new FileOutputStream(jfc.getSelectedFile())) {
					export(file.startSector, fos);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} break;
		case "delete": {
			MCFSFile file = lstFiles.getSelectedValue();
			if (file != null) {
				if (file.bootFile) {
					disk[122] = 0;
					disk[123] = 0;
				}
				freeSectors(file.startSector & 0xFFFF);
				Arrays.fill(disk, file.ptr, file.ptr + 32, (byte)0);
				files.removeElement(file);
				modified = true;
			}
		} break;
		case "toggleboot": {
			MCFSFile bootfile = lstFiles.getSelectedValue();
			if (bootfile == null) return;
			for(int i = 0; i < files.getSize(); i++) {
				MCFSFile file = files.get(i);
				if (file != bootfile) file.bootFile = false;
			}
			if (bootfile.bootFile) {
				disk[122] = 0;
				disk[123] = 0;
				bootfile.bootFile = false;
			} else {
				disk[122] = (byte)bootfile.startSector;
				disk[123] = (byte)(bootfile.startSector >> 8);
				bootfile.bootFile = true;
			}
			modified = true;
			lstFiles.repaint();
		} break;
		}
	}
	
	private void close() {
		Arrays.fill(disk, (byte)0);
		files.clear();
		currentFile = null;
		modified = false;
		this.setTitle("MCFSEdit");
	}
	
	private void reloadFiles() {
		// Read boot file
		short bootSector = (short)(disk[122]&0xFF | disk[123]<<8);
		// Read directory table
		files.clear();
		for(int ptr = 32+6*128; ptr < 16*128; ptr += 32) {
			short sector = (short)(disk[ptr]&0xFF | disk[ptr+1]<<8);
			if (sector != 0) {
				short size = (short)(disk[ptr+2]&0xFF | disk[ptr+3]<<8);
				byte[] fileNameBytes = Arrays.copyOfRange(disk, ptr + 4, ptr + 32);
				int fileNameByteCount = 0;
				for(int i = 0; i < 28; i++, fileNameByteCount++) {
					byte b = fileNameBytes[i];
					fileNameBytes[i] = (byte)(b & 0x7F);
					if ((b & 0x80) != 0) {
						fileNameByteCount++;
						break;
					}
					if (b == 0) break;
				}
				fileNameBytes = Arrays.copyOf(fileNameBytes, fileNameByteCount);
				String fileName = new String(fileNameBytes, StandardCharsets.US_ASCII);
				MCFSFile file = new MCFSFile();
				file.ptr = ptr;
				file.startSector = sector;
				file.sectorSize = size;
				file.bootFile = sector == bootSector;
				file.fileName = fileName;
				files.addElement(file);
			}
		}
	}
	
	private void open(InputStream in) throws IOException {
		// Read disk image into byte array
		Arrays.fill(disk, (byte)0);
		int offset = 0, read;
		while((read = in.read(disk, offset, disk.length - offset)) != -1 && offset < disk.length) {
			offset += read;
		}
		// Throw exception if it is not an MCFS disk
		if (!Arrays.equals(Arrays.copyOfRange(disk, 124, 128), mcfsMagic))
			throw new IOException("Disk image is not a valid MCFS disk");
		// Read volume name
		byte[] nameBytes = Arrays.copyOfRange(disk, 4+6*128, 32+6*128);
		int nameByteCount = 0;
		for(int i = 0; i < nameBytes.length; i++, nameByteCount++) {
			byte b = nameBytes[i];
			if (b == 0) break;
			b &= 0x7F;
			nameBytes[i] = b;
			if ((b & 0x80) == 0x80) break;
		}
		nameBytes = Arrays.copyOf(nameBytes, nameByteCount);
		this.setTitle("MCFSEdit - " + new String(nameBytes, StandardCharsets.US_ASCII));
		reloadFiles();
	}
	
	private void export(short sector, OutputStream out) throws IOException {
		if (sector == 0) return;
		do {
			int sectorPtr = 128 * sector;
			if (sectorPtr > disk.length - 128) throw new IOException("Invalid sector value 0x" + Integer.toHexString(sector));
			sector = (short)(disk[sectorPtr]&0xFF | disk[sectorPtr+1]<<8);
			int byteCount = 126;
			if ((sector & 0xFF00) == 0xFF00) byteCount = sector & 0xFF;
			out.write(disk, sectorPtr + 2, byteCount);
		} while(sector != 0 && (sector & 0xFF00) != 0xFF00);
	}
	
	private int allocFreeSector() {
		int sectorNum = 0;
		for(int i = 4*128; i < 6*128; i++) {
			for(int j = 7; j >= 0; j--, sectorNum++) {
				if (((disk[i]>>j)&1) == 0) {
					disk[i] |= 1 << j;
					return sectorNum;
				}
			}
		}
		return -1;
	}
	
	private int nextFreeDirEntry() {
		for(int i = 32+6*128; i < 16*128; i += 32) {
			short sector = (short)(disk[i]&0xFF | disk[i+1]<<8);
			if (sector == 0) return i;
		}
		return -1;
	}
	
	private int numFreeSectors() {
		int freeCount = 0;
		for(int i = 4*128; i < 6*128; i++) {
			freeCount += 8 - Integer.bitCount(disk[i] & 0xFF);
		}
		return freeCount;
	}
	
	private int numFreeDirEntries() {
		int freeCount = 0;
		for(int i = 32+6*128; i < 16*128; i += 32) {
			short sector = (short)(disk[i]&0xFF | disk[i+1]<<8);
			if (sector == 0) freeCount++;
		}
		return freeCount;
	}
	
	private void import0(InputStream in, String name) throws IOException {
		// Read in file fully
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while((read = in.read(buffer)) != -1) baos.write(buffer, 0, read);
		byte[] data = baos.toByteArray();
		
		// Make sure enough sectors are available
		int bytesFree = numFreeSectors() * 126;
		if (bytesFree < data.length) throw new IOException("Not enough space left on volume");
		
		// Make sure a directory entry is available
		if (numFreeDirEntries() <= 0) throw new IOException("No more directory entries available on volume");
		
		// Write file to disk
		// Get directory entry
		int dir = nextFreeDirEntry();		
		// Compute entry values
		int sectorCount = data.length / 126;
		if ((data.length % 126) != 0) sectorCount++;
		int firstSector = allocFreeSector();
		byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
		if (nameBytes.length > 28) nameBytes = Arrays.copyOf(nameBytes, 28);
		nameBytes[nameBytes.length-1] |= 0x80;
		nameBytes = Arrays.copyOf(nameBytes, 28);
		// Write to directory entry
		disk[dir] = (byte)firstSector;
		disk[dir+1] = (byte)(firstSector>>8);
		disk[dir+2] = (byte)sectorCount;
		disk[dir+3] = (byte)(sectorCount>>8);
		System.arraycopy(nameBytes, 0, disk, dir+4, 28);
		// Write to sectors
		int sector = firstSector;
		int offset = 0;
		do {
			// Determine number of bytes to write
			int toWrite = data.length - offset;
			int next;
			// If >126, alloc next sector
			if (toWrite > 126) {
				toWrite = 126;
				next = allocFreeSector();
			// Else set next to appropriate value
			} else next = 0xFF00 | toWrite;
			
			// Write to sector
			sector *= 128;
			disk[sector] = (byte)next;
			disk[sector+1] = (byte)(next>>8);
			System.arraycopy(data, offset, disk, sector+2, toWrite);
			
			// Offset amount written
			offset += toWrite;
			sector = next;
		} while(offset < data.length);
	}
	
	private void freeSectors(int sector) {
		do {
			int samptr = 4*128 + (sector/8);
			int sambit = 1<<(7-(sector%8));
			disk[samptr] &= ~sambit;
			int sectorptr = sector * 128;
			sector = disk[sectorptr]&0xFF | (disk[sectorptr+1]<<8)&0xFF00;
			Arrays.fill(disk, sectorptr, sectorptr+128, (byte)0);
		} while(sector != 0 && ((sector & 0xFF00) != 0xFF00));
	}

}
