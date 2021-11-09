import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/*
 * Detta program �r en SFTP-klient som kan koppla upp sig mot en SSH-server med hj�lp av Jsch-library. 
 */

public class SFTPClient extends JFrame{
	
	private Session session;
	private ChannelSftp channelSftp;
	private JList<String> list;
	private DefaultListModel <String> dlm;
	private Vector <ChannelSftp.LsEntry> fileList;
	private JFrame frame;
	private JTextField username;
	private JPasswordField password;
	private JTextField host;
	private JButton btn3;
	private Boolean active;
	
public static void main(String[] args) {		// programmets entrypoint, startar en ny SFTPClient
	new SFTPClient();
}

public SFTPClient() {							// bygger programmets GUI och l�gger till listeners
	dlm = new DefaultListModel<>();
	list = new JList<>(dlm);
	list.addMouseListener(new clickHandler());
    username = new JTextField();
    password = new JPasswordField();
    host = new JTextField();
    frame = new JFrame();  
    active = false;
	JPanel inputs = new JPanel();
    GridLayout grid = new GridLayout(5, 2); 
    JButton btn1 = new JButton("Upload");
    btn1.addActionListener(new UploadHandler());
    JButton btn2 = new JButton("Download");  
    btn2.addActionListener(new DownloadHandler());
    btn3 = new JButton("Login");
    btn3.addActionListener(new LoginHandler());
    JButton btn4 = new JButton("Delete");
    btn4.addActionListener(new DeleteHandler());
    JScrollPane textarea = new JScrollPane(list);  
    inputs.setLayout(grid);
    inputs.add(new JLabel("Username"));
    inputs.add(username);
    inputs.add(new JLabel("Password"));
    inputs.add(password);
    inputs.add(new JLabel("Host"));
    inputs.add(host);
    inputs.add(btn1);   
    inputs.add(btn2);   
    inputs.add(btn3);
    inputs.add(btn4);
    JPanel top = new JPanel();
    GridLayout topgrid= new GridLayout(1,5);
    JButton btn5 = new JButton("Back");
    btn5.addActionListener(new BackHandler());
    JButton btn6 = new JButton("Rename/Move");
    btn6.addActionListener(new RenameOrMoveHandler());
    JButton btn7 = new JButton("Create new file");
    btn7.addActionListener(new CreateFileHandler());
    JButton btn8 = new JButton("Create new directory");
    btn8.addActionListener(new CreateDirHandler());
    top.setLayout(topgrid);
    top.add(btn5);
    top.add(btn6);
    top.add(btn7);
    top.add(btn8);
    frame.add("North", top);
    frame.add("South", inputs);
    frame.add("Center", textarea);
    frame.setDefaultCloseOperation(3);
    frame.setSize(650, 600);
    frame.setVisible(true);
}
	
private void connect(String str1, String str2, String str3) {		// Anv�nder angivna v�rden i textf�lten f�r att skapa en ny connection, port 22  anv�nds av default
	// Anv�ndarens angivna v�rden
	String user = str1;
	String password = str2;		
	String host = str3;
	Properties config = new Properties();
	config.put("StrictHostKeyChecking", "no");		// Anv�nder password authentication ist�llet
	JSch jSch = new JSch();
	
	try 
	{
		session = jSch.getSession(user, host);
		session.setPassword(password);
		session.setConfig(config);
		session.connect();
		channelSftp = (ChannelSftp) session.openChannel("sftp");		//�ppnar en session mot servern och skapar en ny SFTP-channel som kommer anv�ndas f�r att utf�ra kommandon
		channelSftp.connect();
		frame.setTitle(channelSftp.pwd());
		updateList();
		active = true;
		btn3.setText("Logout");
	} catch (Exception e) 
	{
		active = false;
		btn3.setText("Login");
		e.printStackTrace();
	}
}

private void disconnect() {		// Bryter uppkopplingen mot servern
	channelSftp.disconnect();
	session.disconnect();
}

private void updateList() {	// Uppdaterar JList med den aktuella mappens filer/mappar
	try 
	{
		dlm.clear();											// Rensar JListans dlm
		fileList = channelSftp.ls(channelSftp.pwd());		// H�mtar objekten i den aktuella mappen
		Collections.sort(fileList);							// Sorterar dom i bokstavsordning
		for(int i = 0; i < fileList.size(); i++) 				
		{
			// Itererar �ver objekten och l�gger till alla elements vars filnamn inte b�rjar med "." i JListens dlm
			ChannelSftp.LsEntry isEntry = fileList.get(i);
			if(!isEntry.getFilename().startsWith("."))
			dlm.addElement(isEntry.getFilename());
		}
		
		frame.setTitle(channelSftp.pwd());		// S�tter mappens namn som GUI titel
		
	}catch (SftpException e) 
	{		
		e.printStackTrace();
	}	
}

private void previousFolder() {	 // G�r tillbaka ett steg i mapphierarkin
	try 
	{
		channelSftp.cd("..");		// Anv�nder cd kommandot f�r att tillbaka ett steg
		updateList();
			
	} 	catch (SftpException e) 
	{
		e.printStackTrace();
	}
}

private void accessFolder() { 	// G�r in i vald mapp
	try 
	{
		channelSftp.cd(channelSftp.pwd() + "/" + list.getSelectedValue());  // Byter directory via cd kommandot
		updateList();
		
	}catch (SftpException e) 
	{
		e.printStackTrace();	
	}
}

private void copyRemoteDir(String source, String dest) { 	// Itererar igenom en mapp och kopierar dess filer och submappar till lokal destinationsmapp
   try 
   {
	   Vector<ChannelSftp.LsEntry> list = channelSftp.ls(source); 			// H�mta alla objekt fr�n mappen som ska kopieras

		for (ChannelSftp.LsEntry file : list) 								// Iterera �ver objekten f�r att best�mma deras filtyper
	    { 
	        if (!file.getAttrs().isDir()) 				 					// Om objektet inte �r en submapp
	        {																																					
	            if (!(new File(dest + "/" + file.getFilename())).exists()) 		// och om objektet inte redan existerar i destinationsmappen
	            {
	                channelSftp.get(source + "/" + file.getFilename(), dest + "/" + file.getFilename());		 // H�mta objektet och lagra det i destinationsmappen
	            }
	        } else if (!(".".equals(file.getFilename()) || "..".equals(file.getFilename()))) 		// Om objektet �r en submapp och inte heter "." eller ".."
	        {
	            new File(dest + "/" + file.getFilename()).mkdirs(); 							// Skapa mapp med samma namn i destinationsmappen
	            copyRemoteDir(source + "/" + file.getFilename(), dest + "/" + file.getFilename()); 			// K�r om denna metod fast i denna submapp f�r att h�mta dess inneh�ll
	        }
	    }
	}catch(Exception e) 
   {
		e.printStackTrace();
   }
   
}

private void copyLocalDir(String source, String dest)  { 	// Itererar igenom en mapp och kopierar dess filer och submappar till remote destinationsmapp
	try 
	{
		File[] localFile = new File(source).listFiles();	// H�mta alla filer/submappar i den valda lokala mappen	

		for(File file : localFile)							// Iterera �ver filerna/submapparna och kolla deras filtyper
    	{   
    		if(!file.isDirectory())					// Om det inte �r en submapp
    		{
    			if (!(new File(dest + "/" + file.getName())).exists()) 	// och om den inte redan existerar i nya mappen p� servern
    			{
    				channelSftp.put(source + "/" + file.getName(), dest);    // ladda upp filen till nya mappen p� servern
    			}
    		}else if(!(".".equals(file.getName()) || "..".equals(file.getName())))		// om det �r en mapp och den inte har filnamnet "." eller ".."
    		{
    			channelSftp.mkdir(dest + "/" + file.getName());					// skapa en ny submapp i den nya mappen p� servern
    			copyLocalDir(source +  "/" + file.getName(), dest + "/" + file.getName());		// k�r samma metod p� submappen 
    		}  		
    	}
	}catch(Exception e)
	{
		e.printStackTrace();
	}
}

private void removeDir(String dir) {		// Tar bort vald mapp fr�n servern
	try 
	{
	    Vector<ChannelSftp.LsEntry> list = channelSftp.ls(dir); // H�mta alla objekt fr�n mappen som ska raderas	   

	    for (ChannelSftp.LsEntry file : list) 				// Iterera �ver objekten f�r att best�mma deras filtyper.
	    {
	        if (!file.getAttrs().isDir()) 						//  Om objektet inte �r en submapp
	        { 
	        	channelSftp.rm(dir + "/" + file.getFilename()); 		// Ta bort filen
	        }
	        else if (!(".".equals(file.getFilename()) || "..".equals(file.getFilename()))) // Om objektet �r en submapp och inte heter "." eller ".."
	        { 
	            try 
	            {
	            	channelSftp.rmdir(dir + "/" + file.getFilename());  			// F�rs�k att ta bort submappen
	            }catch (Exception e) 
	            {																		 // Ifall error sker betyder det att submappen inte �r tom
	            	removeDir(dir + "/" + file.getFilename()); 				// K�r is�fall om metoden p� submappen
	            }
	        }
	    }
	    channelSftp.rmdir(dir); 												// Efter mappens inneh�ll har t�mts p� submappar och filer, ta bort hela mappen
	    updateList();
	} catch (SftpException e) 
	{
	    e.printStackTrace();
	}
}

private void removeFile() {				// Tar bort vald fil fr�n servern
	try 
	{
		channelSftp.rm(channelSftp.pwd() + "/" + list.getSelectedValue());		// Tar bort filen
		updateList();
	} catch (SftpException e) 
	{
		e.printStackTrace();
	}
}

private void uploadFile(File file){		// Laddar upp vald fil till servern
	{
		try 
		{
			channelSftp.put(file.getAbsolutePath(), channelSftp.pwd());		// laddar upp filen
			updateList();
		} catch (SftpException e) 
		{
			e.printStackTrace();
		}
	}
}

private void uploadDir(File dir) 		// Laddar upp vald mapp till servern och kopierar �ver dess submappar och filer
{
	try 
	{
		channelSftp.mkdir(channelSftp.pwd() + "/" + dir.getName());		// Skapar en ny mapp p� servern med samma namn som vald lokal mapp
		copyLocalDir(dir.getAbsolutePath(), channelSftp.pwd() + "/" + dir.getName());		// Kopierar �ver allt inneh�ll i valda lokala mappen till nya mappen p� servern
		updateList();
	} catch (SftpException e) 
	{
		e.printStackTrace();
	}
}

private void getFile() {				// Hanterar nedladdning av vald fil, destination v�ljs ut och sedan h�mtas filen 
	
	//	H�r v�ljs destination f�r filen som ska h�mtas
	JFileChooser fc = new JFileChooser();
	fc.setSelectedFile(new File(list.getSelectedValue().toString()));
	int userSelection = fc.showSaveDialog(frame);
	if (userSelection == JFileChooser.APPROVE_OPTION)  
	{
		File file = fc.getSelectedFile();
		try 
		{	
			channelSftp.get(channelSftp.pwd() + "/" + list.getSelectedValue(), file.getAbsolutePath());			// Filen h�mtas och lagras i destinationen
			
		} catch (SftpException e) 
		{
			e.printStackTrace();
		}
	}
}

private void getDir() {				// Hanterar nedladdning av mapp, destinations v�ljs ut, ny lokal mapp skapas och en kopieringsmetod fyller den med inneh�ll
	
	//	H�r v�ljs destination f�r mappen som ska h�mtas
	JFileChooser fc = new JFileChooser();
	fc.setSelectedFile(new File(list.getSelectedValue().toString()));
	int userSelection = fc.showSaveDialog(frame);
	if (userSelection == JFileChooser.APPROVE_OPTION)  
	{
		File files = fc.getSelectedFile(); 
		try 
		{
			new File(files.getAbsolutePath()).mkdirs(); 		// Destinationsmapp utan inneh�ll skapas
			copyRemoteDir(channelSftp.pwd() + "/" + list.getSelectedValue(), files.getAbsolutePath());		// Metod som itererar igenom mappen som ska h�mtas inneh�ll och kopierar �ver det till den nya lokala destinationsmappen 
			
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}

class LoginHandler implements ActionListener 		// listener som antingen loggar in eller loggar ut anv�ndaren
{
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {
    	if(!active) 		// Om active �r false s� ska anv�ndaren loggas in, annars anv�ndaren denne ut
    	{
    		// h�mtar all relevant inloggningsinfo
	    	String str1 = username.getText();
	    	String str2 = password.getText();
	    	String str3 = host.getText();    	
	    	connect(str1, str2, str3);				// �kallar metod connect som loggar in anv�ndaren
    	}
    	else 
    	{
    		// anv�ndaren loggas ut
    		dlm.clear();
    		disconnect();
    		active = false;
    		btn3.setText("Login");
    	}
	}   	
}

class clickHandler extends MouseAdapter				// listener som vid tv� musklick �kallar en metod f�r att g� in i vald mapp
{
    @Override
    public void mouseClicked(MouseEvent e) 
    {
    	if(e.getClickCount() == 2) 
    	{    		
    		accessFolder();  	
    	}
    }
}

class DownloadHandler implements ActionListener		// listener som j�mf�r filnamn mellan objekt i JLists dlm-samling och en samling ChannelSftp.LsEntry objekt lagrade i fileList h�mtade genom metoden channelSftp.ls(), n�r den hittar en match laddas det ner
{
	@Override
	public void actionPerformed(ActionEvent actionEvent)
	{
	   	for(int i = 0; i < fileList.size(); i++) 		// iterera �ver alla ChannelSftp.LsEntry-objekt och hitta den som har samma filnamn som valda JList-objektet
    	{
    		ChannelSftp.LsEntry entry = fileList.get(i);	
			if(entry.getFilename().contains(list.getSelectedValue().toString()))
			{
				if(!entry.getAttrs().isDir()) 			// om objektet inte �r en mapp, k�r getFile
				{
					getFile();
					break;
				}
				else 
				{
					getDir();						// om objektet �r en mapp, k�r getDir
					break;
				}
			}
    	}
	}
}

class UploadHandler implements ActionListener		// listener som �ppnar en dialogruta d�r man f�r v�lja vilken lokal fil/mapp som ska laddas upp
{
	@Override
	public void actionPerformed(ActionEvent actionEvent)
	{
		// V�lj ut fil/mapp som ska laddas upp
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int userSelection = fc.showOpenDialog(frame);
		
		if (userSelection == JFileChooser.APPROVE_OPTION)  
		{
			File file = fc.getSelectedFile();
			
			if(!file.isDirectory())		// om det inte �r en mapp, k�r uploadFile
			{
				uploadFile(file);
			}
			else 
			{
				uploadDir(file);		// om det �r en mapp, k�r uploadDir
			}
		}
	}
}

class DeleteHandler implements ActionListener  		// listener som j�mf�r filnamn mellan objekt i JLists dlm-samling och en samling ChannelSftp.LsEntry objekt lagrade i fileList h�mtade genom metoden channelSftp.ls(), n�r den hittar en match tas det bort
{
	
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {
    	for(int i = 0; i < fileList.size(); i++) 		// iterera �ver alla ChannelSftp.LsEntry-objekt och hitta den som har samma filnamn som valda JList-objektet
    	{
    		ChannelSftp.LsEntry entry = fileList.get(i);
			if(entry.getFilename().contains(list.getSelectedValue().toString()))
			{   	
		    	if(entry.getAttrs().isDir())		// om objektet �r en mapp, k�r RemoveDir
				try 
			    	{
						removeDir((channelSftp.pwd() + "/" + list.getSelectedValue()));
						break;
					} catch (SftpException e) 
			    	{
						e.printStackTrace();
					}
				else 
		    	{
		    		removeFile();		// om objektet �r en fil, k�r RemoveFile						
					break;
		    	}
			}
    	}
    }
}

class BackHandler implements ActionListener  		// listener som �kallar en metod som g�r tillbaka till f�reg�ende mapp
{
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {
    	previousFolder();
	}   	
}

class RenameOrMoveHandler implements ActionListener	// listener som kan byta namn och plats p� en fil/mapp
{
	
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {   	
    	String newname = JOptionPane.showInputDialog(null, "Write full new path and/or new name", null);	// H�r anges (om s� �nskas) ny path och filens/mappens nya namn
    	if(newname != null) 
    	{
    		if(!newname.isEmpty())
	    	for(int i = 0; i < fileList.size(); i++) 					// iterera �ver alla ChannelSftp.LsEntry-objekt och hitta den som har samma filnamn som valda JList-objektet
	    	{
	    		ChannelSftp.LsEntry entry = fileList.get(i);
				if(entry.getFilename().contains(list.getSelectedValue().toString()))
				{
					try 
					{
						channelSftp.rename(channelSftp.pwd() + "/" + list.getSelectedValue(),  newname);		// Om man hittar en match, byt namn p� filen/mappen
						updateList();
						break;
					} catch (SftpException e) 
					{
						e.printStackTrace();
					}
				}
	    	}  
    	}
   }
}

class CreateFileHandler implements ActionListener	// listener som skapar en ny fil lokalt och laddar sedan upp den p� servern (d� SFTP inte hade en metod f�r att skapa filer p� servers, bara mappar)
{
	
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {
    	String fileName = JOptionPane.showInputDialog(null, "Enter file name", null);	// h�r anges filens namn
    	if(fileName != null)
    	{
    		if(!fileName.isEmpty())
    		{
		    	File file = new File(channelSftp.lpwd() + "/" + fileName);		// filen skapas lokalt
		    	try {
					if(file.createNewFile())
					{
						try 
						{
							channelSftp.put(file.getAbsolutePath(), channelSftp.pwd());		// filen laddas upp och GUI uppdateras
							updateList();
							file.delete();								// lokala filen raderas
						} catch (SftpException e) 
						{
							e.printStackTrace();
						}
					}
				} catch (IOException e) 
		    	{
					e.printStackTrace();
				}
	    	}
    	}
    }
}

class CreateDirHandler implements ActionListener	// listener som skapar en ny mapp p� servern
{
	
    @Override
    public void actionPerformed(ActionEvent actionEvent) 
    {
    	String fileName = JOptionPane.showInputDialog(null, "Enter directory name", null);		// h�r anges mappens namn
    	if(fileName != null)
    	{
    		if(!fileName.isEmpty())
    		{
				try 
				{
					channelSftp.mkdir(channelSftp.pwd() + "/" + fileName);					// mappen skapas och GUI uppdateras
					updateList();
				} catch (SftpException e) 
				{
					e.printStackTrace();
				}   
	    	}
    	}
    }
}
}