import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

class RequestProcessor extends Thread
{
    private Socket socket;
    private String id;
    private FTServerFrame fsf;
     
    RequestProcessor(Socket socket,String id,FTServerFrame fsf)
    {
        this.fsf=fsf;
        this.id=id;
        this.socket=socket;
        start();
    }

    public void run()
    {
        try
        {
            SwingUtilities.invokeLater(new Runnable(){
                public void run()
                {
                    fsf.updateLog("Client connected and id alloted is "+id);
                }
            });
            InputStream inputStream=socket.getInputStream();
            OutputStream outputStream=socket.getOutputStream();
            int bytesToRecieve=1024;
            byte temp[]=new byte[1024];
            byte header[]=new byte[1024];
            int bytesReadCount;
            int i,j,k;
            i=0;
            j=0;
            while(j<bytesToRecieve)
            {
                bytesReadCount=inputStream.read(temp);
                if(bytesReadCount==-1) continue;
                for (k = 0; k < temp.length; k++)
                {
                    header[i++]=temp[k];
                }
                j=j+bytesReadCount;
            }
            int lengthOfFile=0;
            i=0;
            j=1;
            while(header[i]!=',')
            {
                lengthOfFile=lengthOfFile+(header[i]*j);
                j=j*10;
                i++;
            }
            i++;
            StringBuffer sb=new StringBuffer();
            while(i<=1023)
            {
                sb.append((char)header[i++]);
            }
            String fileName=sb.toString().trim();
            int lof=lengthOfFile;
            SwingUtilities.invokeLater(()->{
                fsf.updateLog("Recieving file:"+fileName+" of length "+lof);
            });
            File file=new File("uploads"+File.separator+fileName);
            if(file.exists()) file.delete();

            FileOutputStream fos=new FileOutputStream(file);
            byte ack[]=new byte[1];
            ack[0]=1;
            outputStream.write(ack,0,1);
            outputStream.flush();

            int chunkSize=4096;
            byte bytes[]=new byte[chunkSize];
            i=0;
            long m;
            m=0;
            while(m<lengthOfFile)
            {
                bytesReadCount=inputStream.read(bytes);
                if(bytesReadCount==-1) continue;
                fos.write(bytes,0,bytesReadCount);
                fos.flush();
                m=m+bytesReadCount;
            }
            fos.close();

            ack[0]=1;
            outputStream.write(ack, 0, 1);
            outputStream.flush();
            socket.close();
            SwingUtilities.invokeLater(()->{
                fsf.updateLog("File saved to: "+file.getAbsolutePath());
                fsf.updateLog("Connection with client whose id is: "+id+" closed");
            });

        }catch(Exception e)
        {
            System.out.println(e);
        }
    }
}
class FTServer extends Thread
{
    private ServerSocket serverSocket;
    private FTServerFrame fsf;

    FTServer(FTServerFrame fsf)
    {
        this.fsf=fsf;
    }

    public void run()
    {
        try 
        {
           serverSocket=new ServerSocket(5502);
           startListening();
        }catch(Exception e) 
        {
            System.out.println(e);
        }
    }

    public void shutDown()
    {
        try 
        {
            serverSocket.close();
        }catch(Exception e) 
        {
            System.out.println(e);
        }
    }

    private void startListening()
    {
        try
        {
            Socket socket;
            RequestProcessor requestProcessor;
            while(true)
            {
                System.out.println("Server Started");
                SwingUtilities.invokeLater(()->{
                    fsf.updateLog("Server started and is listening on port 5502");
                });
                socket=serverSocket.accept();
                requestProcessor=new RequestProcessor(socket,UUID.randomUUID().toString(),fsf);
            }
        }catch(Exception e)
        {
            System.out.println("Server stopped listening");
            System.out.println(e);
        }
    }
}

class FTServerFrame extends JFrame implements ActionListener
{
    private Container container;
    private JButton button;
    private JScrollPane jsp;
    private JTextArea jta;
    private boolean serverState;
    private FTServer server;
    FTServerFrame()
    {
        container=getContentPane();
        button=new JButton("Start");        
        jta=new JTextArea();
        jsp=new JScrollPane(jta, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        container.setLayout(new BorderLayout());
        container.add(jsp,BorderLayout.CENTER);
        container.add(button,BorderLayout.SOUTH);
        button.addActionListener(this);
        setLocation(250,250);
        setSize(400,400);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        serverState=false;
    }

    public void updateLog(String message)
    {
        jta.append(message+"\n");
    }

    public void actionPerformed(ActionEvent ev)
    {
        if(serverState==false)
        {
            server=new FTServer(this);
            server.start();
            serverState=true;
            button.setText("Stop");
        }
        else
        {
            server.shutDown();
            serverState=false;
            button.setText("Start");
            jta.append("Server stopped\n");
        }
    }

    public static void main(String[] args)
    {
        FTServerFrame fsf=new FTServerFrame();
    }
}


