
/*Program to download the mails from mail servers and then to store

  them in tdb store using rdf model*/
//import all the classes needed  
/*  The MIT License (MIT)

Copyright (c) IIIT-DELHI 
authors:
HEMANT JAIN "hjcooljohny75@gmail.com"
ANIRUDH NAIN 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. 

 * 
 */


package in.ac.iiitd.mazil;
import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.*;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import static com.hp.hpl.jena.query.ReadWrite.READ ;
import static com.hp.hpl.jena.query.ReadWrite.WRITE ;
import com.hp.hpl.jena.query.ReadWrite ;
import com.hp.hpl.jena.query.Dataset ;
import com.hp.hpl.jena.tdb.TDBFactory ;
import in.ac.iiitd.mazil.EMAILRDF; // import this to add properties as entities of email
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import java.net.URL;
import java.net.MalformedURLException;


public class Emaildownload
{ 
	private static boolean textIsHtml = false;
    //	method to get contents of multipart email
	private static String getText(Part p) throws MessagingException, IOException 
    {
        if (p.isMimeType("text/*")) 
        {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) 
        {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++)
            {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain"))
                {
                    if (text == null)
                        text = getText(bp);
                    continue;
                }
                else if (bp.isMimeType("text/html")) 
                {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                }
                else 
                {
                    return getText(bp);
                }
            }
            return text;
        }
        else if (p.isMimeType("multipart/*")) 
        {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) 
            {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }
   
    public static void mai(String[] arg) throws MessagingException, IOException 
    {
        String[] credentials=new String[4];             //getting the credentials emailid,password,folder of mail and text file name
        int k=0;
        for (String s: arg) 
        {
            System.out.println(s);
            credentials[k]=s;
            k++;
	        if(k==4)
	            break;
        }
        IMAPFolder folder = null;
        Store store = null;
        String subjec = "nosubject";
        Flag flag = null;
        String dat="x",encod="x",senderaddr="x",receiveraddr="x",cont="x";//initializing
        File Mazil = new File(System.getProperty("user.home")+File.separator+"Mazil");
        if (!Mazil.exists()) 
        {
            System.out.println("creating directory: " + Mazil);
            boolean result = Mazil.mkdir();  
            if(result) 
            {    
                System.out.println("Mazil DIR created");  
            }
        }
        
        
        
        
        //Directory where the tdb files will be stored
        File EMAILADDRESS = new File(System.getProperty("user.home")+File.separator+"Mazil"+File.separator+"EMAILADDRESS");
        long lastuid=0;
        long lastvalidity=606896160;        //initializing just for checking uid validity
        String references="";
        String cont2;
        String link;
        
        // if the directory does not exist, create it
        if (!EMAILADDRESS.exists()) 
        {
            System.out.println("creating directory: " + EMAILADDRESS);
            boolean result = EMAILADDRESS.mkdir();  
            if(result) 
            {    
                System.out.println("DIR created");  
            }
        }
        String directory = System.getProperty("user.home")+File.separator+"Mazil"+File.separator+"EMAILADDRESS" ;
      
        try 
        {   //connecting to the server to download the emails
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");
            Session session = Session.getDefaultInstance(props, null);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com",credentials[0], credentials[1]);
            String foldername=credentials[2];
            folder = (IMAPFolder) store.getFolder(foldername);// This works for both email account
            /* Others GMail folders :
            * [Gmail]/All Mail   This folder contains all of your Gmail messages.
            * [Gmail]/Drafts     Your drafts.
            * [Gmail]/Sent Mail  Messages you sent to other people.
            * [Gmail]/Spam       Messages marked as spam.
            * [Gmail]/Starred    Starred messages.
            * [Gmail]/Trash      Messages deleted from Gmail.
            */
            UIDFolder uf = (UIDFolder)folder;
            if(!folder.isOpen())
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            long n=uf.getUIDValidity();                 //getting the UIDvalidity of the current folder
            System.out.println("UIDvalidity:"+n);       
            int a=0;
            String line;
            String liner[]=new String[2];;
            BufferedReader bfr;    
            //bfr=new BufferedReader(new InputStreamReader(System.in));
            String OS = System.getProperty("os.name").toLowerCase();
            System.out.println(OS);
            String content=String.valueOf(n)+System.getProperty("line.separator")+messages.length;
            String fileName=System.getProperty("user.home")+File.separator+"Mazil"+File.separator+credentials[3];
           
            File file=new File(fileName);       
            if(!file.exists())
            {
	              System.out.println("filecreated");
	             
                file.createNewFile();
            }       
            try
            {
                bfr=new BufferedReader(new FileReader(file));
                while((line=bfr.readLine())!=null)
                {
                    liner[a]=line;
                    a++;
                }
                while((line=bfr.readLine())!=null)
                {
                   System.out.println(line);
                }
                FileWriter fw=new FileWriter(file,false);            
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(content); 
                bw.close();     
                bfr.close();
                //fw.close();
            }
            catch(FileNotFoundException fex)
            {
                fex.printStackTrace();
            } 
            if(liner[0]==null)
                liner[0]="0";
            if(liner[1]==null)
                liner[1]="0";
            lastvalidity=(long)Long.parseLong(liner[0]);
            lastuid=(long)Long.parseLong(liner[1]);
            //System.out.println("hi")  ;
            //System.out.println(lastvalidity);
            //System.out.println(lastuid );
            if(n==lastvalidity||lastvalidity==0)
            {
            }
            else
            {
                System.out.println("database inconsistent");
                return;
            }
            System.out.println("No of Messages : " + folder.getMessageCount());
            System.out.println("No of Unread Messages : " + folder.getUnreadMessageCount());
            //System.out.println(messages.length);
            //Displaying the info. of the messages
            for (int i=(int)lastuid; i < messages.length;i++) 
            {
                //System.out.println(i);
                subjec="nosubject" ;dat="x";encod="x";senderaddr="x";receiveraddr="x";cont="x";
                MimeMessage msg = (MimeMessage) messages[i];
                IMAPMessage msg2 = (IMAPMessage) messages[i];
                //creating rdf model of the message
                /*typecating of these email entities to strings so
                  that they can be placed as arguments in addProperty
                  function
                */
                //checking for null values to prevent errors
                //System.out.println("hi");
                references="";
                if( msg.getHeader("References")!=null)
                {
                	String[] headers = msg.getHeader("References");
                	System.out.println("headers");
                	for(int ab=0;ab<headers.length;ab++)
                		{	 
                		headers[ab]=headers[ab].replace("\r","");
                		headers[ab]=headers[ab].replace("\n","");
                		headers[ab]=headers[ab].replace(" ",",");
                			references=headers[ab];
                		}references+=","+msg.getMessageID();
                	System.out.println(references);
                }
                String bcc="",cc="";
                if(msg.getRecipients(Message.RecipientType.CC)!=null)
                {   
                    int j=0;
                    //System.out.println(j);
                    while(j < msg.getRecipients(Message.RecipientType.CC).length)
                    {
                        cc =cc.concat(msg.getRecipients(Message.RecipientType.CC)[j].toString());
                        cc =cc.concat(",");
                        j++;
                        //System.out.println(j);
                    }  
                }
                else
                { 
                    cc="novalue";
                }
                //System.out.println(cc);
                if(msg.getRecipients(Message.RecipientType.BCC)!=null)
                {   
                    int j=0;
                    //System.out.println(j);
                    while(j < msg.getRecipients(Message.RecipientType.BCC).length)
                    {  
                        bcc =bcc.concat(msg.getRecipients(Message.RecipientType.BCC)[j].toString());
                        bcc =bcc.concat(",");
                        j++;
                        //System.out.println(j);
                    }
                }
                else
                { 
                    bcc="novalue";
                }
                //System.out.println(bcc);
                int msgsize=msg.getSize();
                String msize=String.valueOf(msgsize); 
                //System.out.println(msgsize  );
                String replyto = msg2.getInReplyTo();
                //System.out.println(replyto);
                String replyname;
		            if(replyto==null)
                    replyto="no value";
                if(replyto.indexOf("<")!=-1)
                {
                    int z=replyto.indexOf("<");
                    String[] parts = replyto.split("<");
                    replyname=parts[0];
                    String[] part = parts[1].split(">");
                    replyto=part[0];

                }
                else 
                {
                    replyname="unknown";
                }   
                System.out.println(replyname);
                System.out.println(replyto);
                if(replyto==null)
                    replyto="no reply";
                String filename="";
                String contentType = msg.getContentType();
                //for attachement name
                int no=0;
                if (contentType.contains("multipart"))
                {
                    // this message may contain attachment
                    Multipart multiPart = (Multipart) msg.getContent();
                    for (int l = 0; l < multiPart.getCount(); l++) 
                    {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(l);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) 
                        {
                            no++;
                            filename=filename.concat(part.getFileName());
                            filename=filename.concat(",");
                        }
                    }
                    if(filename==""||filename==null)
                    {
                        filename="no attachment";
                    }
                }
                else
                {
                    filename="no attachment";
                }
                 String nos=String.valueOf(no);  
                //System.out.println(filename);
                long ui = uf.getUID(msg);
                String uid=String.valueOf(ui);  
                //Attachment att = new Attachment( msg );
                //String filename = att.getFilename();
                //System.out.println(filename);
                cont=getText(msg);
                cont2=cont;
                link="";
             // separete input by spaces ( URLs don't have spaces )
                String [] divide = cont2.split("\\s");
                int flag1=0;
                // Attempt to convert each item into an URL.   
                for( String item : divide ) 
                {  
                	try 
                	{
                		URL url = new URL(item);
                		// If possible then replace with anchor...
                		System.out.print("URL is"+url );
                		link=url+",";
                		flag1=1;
                    }
                	catch (MalformedURLException e) 
                	{
                    // If there was an URL that was not it!...
                  
                	}
                }
                	

                System.out.println();
                if(cont==null)
                    cont="no value";
                String fromfull="";
                //System.out.println(cont);
                if(msg.getFrom()!=null)
                {
                    int j=0;
                    //System.out.println(j);
                    while(j < msg.getFrom().length)
                    {
                        fromfull =fromfull.concat(msg.getFrom()[j].toString());
                        fromfull =fromfull.concat(",");
                        j++;
                        //System.out.println(j);
                    }
                }
                else
                {
                   fromfull="novalue";
                }
                System.out.println(fromfull);
                senderaddr=msg.getFrom()[0].toString();
                if(senderaddr==null)
                     senderaddr="no value";
                String sendername;
                if(senderaddr.indexOf("<")!=-1)
                {
                    int z=senderaddr.indexOf("<");
                    String[] parts = senderaddr.split("<");
                    sendername=parts[0];
                    String[] part = parts[1].split(">");
                    senderaddr=part[0];

                }
                else 
                {
                    sendername="unknown";
                } 
                System.out.println(sendername);
                System.out.println(senderaddr);
                //System.out.println(senderaddr);
                //System.out.println(msg.getAllRecipients()[0].toString());
                String tofull="";
                if(msg.getAllRecipients()!=null)
                { 
                    int j=0;
                    //System.out.println(j);
                    while(j < msg.getAllRecipients().length-1)
                    {
                        tofull =tofull.concat(msg.getAllRecipients()[j].toString());
                        tofull =tofull.concat(",");
                        j++;
                        break;
                        //System.out.println(j);
                    }
                }
                else
                {
                   tofull="novalue";
                }
                System.out.println(tofull);
                if(msg.getAllRecipients()==null||msg.getAllRecipients()[0].toString()=="")
                    receiveraddr="no value";
                else
                    receiveraddr=msg.getAllRecipients()[0].toString();
	              tofull= receiveraddr;
                System.out.println(receiveraddr);
                String receivername;
                if(receiveraddr.indexOf("<")!=-1)
                {
                    int z=receiveraddr.indexOf("<");
                    String[] parts = receiveraddr.split("<");
                    receivername=parts[0];
                    String[] part = parts[1].split(">");
                    receiveraddr=part[0];

                }
                else 
                {
                    receivername="unknown";
                }

                System.out.println(receiveraddr);
                System.out.println(receivername);
                dat=msg.getReceivedDate().toString();
                if(dat==null)
                    dat="no value";
                System.out.println(dat); 
                String day,month,dateno,time,timezone,year,mon="00",timezoneno="",finaldatetime;
                String[] parts = dat.split(" ");
                day=parts[0];
                month=parts[1];
                dateno=parts[2];
                time=parts[3];
                timezone=parts[4];
                year=parts[5];
                System.out.println(month);
                System.out.println(timezone);            
                if("Jan".equals(month))
                {
                    mon="01";
                }
                if("Feb".equals(month))
                {
                    mon="02";
                }
                if("Mar".equals(month))
                {
                    mon="03";
                }
                if("Apr".equals(month))
                {
                    mon="04";
                }
                if("May".equals(month))
                {
                    mon="05";
                }
                if("Jun".equals(month))
                {
                    mon="06";
                }
                if("Jul".equals(month))
                {
                    mon="07";
                }
                if("Aug".equals(month))
                {   
                    mon="08";
                }
                if("Sep".equals(month))
                {
                    mon="09";
                }
                if("Oct".equals(month))
                {
                    mon="10";
                }
                if("Nov".equals(month))
                {
                    mon="11";
                }
                if("Dec".equals(month))
                {
                    mon="12";
                }
                if("IST".equals(timezone))
                    timezoneno="+05:30";
                finaldatetime=year+"-"+mon+"-"+dateno+"T"+time+timezoneno;
                System.out.println(finaldatetime);

                encod =msg.getEncoding();
                if(encod==null)
                  encod="8bit";
                if(msg.getSubject()==null||msg.getSubject()=="")
                  subjec="no value";
                else
                  subjec=msg.getSubject();
                try
                {
                    bfr=new BufferedReader(new FileReader(file));
                    FileWriter fw=new FileWriter(file,false);            
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(String.valueOf(n)+System.getProperty("line.separator")+i); 
                    bw.close();     
                    bfr.close();
                    //fw.close();

                }
                catch(FileNotFoundException fex)
                {
                    fex.printStackTrace();
                }
                Dataset ds = TDBFactory.createDataset(directory) ;
                //create default rdf model
                  //create the dataset for the tdb store
        
       
                //write to the tdb dataset
                ds.begin(ReadWrite.WRITE);
                Model model = ds.getDefaultModel() ;
                Literal lo = model.createTypedLiteral(finaldatetime, XSDDatatype.XSDdateTime  ); 
                //System.out.println(subjec);
                Resource mail= model.createResource(msg.getMessageID())
            		.addProperty(EMAILRDF.MSGID,msg.getMessageID()) 
            		.addProperty(EMAILRDF.SUBJECT, subjec)
            		.addProperty(EMAILRDF.LINK, link)
                .addProperty(EMAILRDF.MAILID,credentials[0] ) 
                .addProperty(EMAILRDF.FROMFULL,fromfull )
                .addProperty(EMAILRDF.TOFULL,tofull )
                .addProperty(EMAILRDF.TO,receiveraddr)
                .addProperty(EMAILRDF.FROM,senderaddr)
                .addProperty(EMAILRDF.REC_NAME,receivername)
                .addProperty(EMAILRDF.SEND_NAME,sendername)
                .addProperty(EMAILRDF.ENCODING,encod)
                .addProperty(EMAILRDF.CONTENT,cont)
                .addProperty(EMAILRDF.REF,references)
                //.addProperty(EMAILRDF.DATE,dat)
                .addProperty(EMAILRDF.FOLDER_NAME,foldername)
                .addProperty(EMAILRDF.UID,uid)
                .addProperty(EMAILRDF.IN_REPLYTO,replyto)
                .addProperty(EMAILRDF.IN_REPLYTONAME,replyname)
                .addProperty(EMAILRDF.CC,cc)
                .addProperty(EMAILRDF.BCC,bcc)
                .addLiteral(EMAILRDF.MAIL_SIZE,msgsize)
                .addProperty(EMAILRDF.ATTACHEMENT_NAME,filename)
                .addProperty(EMAILRDF.ATTACHEMENT_NO,nos)
                .addProperty(EMAILRDF.CONTENT_TYPE,msg.getContentType());
                model.add (mail,EMAILRDF.DATE, lo);
                ds.commit();
                ds.end();
            }                     
            // list the statements in the graph
            /*
              StmtIterator iter = model.listStatements();
              int j=0;
              // print out the predicate, subject and object of each statement
              while (iter.hasNext()) {
              System.out.println("*****************************************************************************");
              System.out.println("MESSAGE " + (j + 1) + ":");
              j=j+1;
              Statement stmt      = iter.nextStatement();         // get next statement
              Resource  subject   = stmt.getSubject();   // get the subject
              Property  predicate = stmt.getPredicate(); // get the predicate
              RDFNode   object    = stmt.getObject();    // get the object
            
              //System.out.print(subject.toString());
              System.out.print(" "+ predicate.toString() + " ");
              if (object instanceof Resource) {
                System.out.print(object.toString());
              } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
              }
              System.out.println(" .");
            }
            //System.out.println(msg.getMessageNumber());
            //Object String;
            //System.out.println(folder.getUID(msg)

            //subject = msg.getSubject();

            //System.out.println("Subject: " + subject);
            //System.out.println("From: " + msg.getFrom()[0]);
            //System.out.println("To: "+msg.getAllRecipients()[0]);
            //System.out.println("Date: "+msg.getReceivedDate());
            //System.out.println("Size: "+msg.getSize());
            //System.out.println("Id: "+msg.getMessageID());
            //System.out.println(msg.getFlags());
            //System.out.println("Body: \n"+ msg.getContent());
            //System.out.println(msg.getContentType());
          */
       }        
      finally 
      {  //closing the connection
          if (folder != null && folder.isOpen()) 
          {   
              folder.close(true); 
          }
          if (store != null) 
          { 
              store.close();
          }
      }


      //closing the dataset
     
    }
}
