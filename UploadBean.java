/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intracom.bean;

/**
 *
 * @author Christos Papadiamantopoulos
 */
import com.intracom.model.Employer;
import com.intracom.model.UserOut;
import com.intracom.persistence.PersistentSubmission;
import com.intracom.persistence.PersistentUpload;
import java.io.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import net.sf.jmimemagic.*;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

@ManagedBean
@ViewScoped
public class UploadBean implements Serializable {

 private Logger logger = Logger.getLogger("com.corejsf");
 static Statement stmt;
 //SubmissionList selectedSubmission = new SubmissionList();
 private Employer selectedSubmission = new Employer();
 PersistentSubmission persistentSubmission = new PersistentSubmission();
 OutputStream os = null;
 InputStream is = null;
 File f;
 File f2;
 UserOut userOut = new UserOut();
 Boolean uploadRendered = true;
 Boolean uploadOk = true;
 int temp = 0;
 private Boolean isFileUpload = false;
 PersistentUpload persistentUpload = new PersistentUpload();
 @ManagedProperty(value = "#{secBean}")
 private SecurityBean secBean;
 String fileType = "CSL01";

 public UploadBean() {
 }

 @PostConstruct
 public void init() {
  selectedSubmission.setSid(secBean.getSid());
  isFileUpload = true;
  
  if (secBean.isCheckRoleIsBuild() || secBean.isCheckRoleIsBuildPrivate() || secBean.isCheckRoleIsBuildPublic() || secBean.isCheckRoleIsBuildPrivate2())
   fileType = "CSC01";
 }

 public void initCheckPage() {
  if (secBean.getSid() == null) {
   ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
   try {
    externalContext.redirect(externalContext.getRequestContextPath() + "/faces/secureAll/index.xhtml");
   } catch (IOException ex) {
    Logger.getLogger(ApdBuildBean.class.getName()).log(Level.SEVERE, null, ex);
   }
  }
 }

 public void submit(FileUploadEvent event) throws MagicParseException, MagicException, IOException {
  long start = System.currentTimeMillis();
  Boolean ischechFileSubmition = true;
  Boolean isFileSizeOk = true;
  PropertiesReader reader;
  reader = new PropertiesReader("properties-from-pom.properties");    
  try {
   userOut = persistentUpload.checkFileSubmition(secBean.getSid());
  } catch (SQLException ex) {
   Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
  }
  if (!userOut.getResult().equalsIgnoreCase("0")) {
   uploadOk = false;
   FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, userOut.getFailureMessage(), "");
   FacesContext.getCurrentInstance().addMessage(null, msg);
   ischechFileSubmition = false;
   return;
  }

  String fileName = getFileNameFromPath(event.getFile().getFileName().toString()); 

  if (secBean.isCheckRoleIsBuild() || secBean.isCheckRoleIsBuildPrivate() || secBean.isCheckRoleIsBuildPublic())

   if (secBean.isCheckRoleIsCommon())
    try {
     persistentSubmission.insertToLogFileUpload("10", secBean.getClientIp(), secBean.getUser(), secBean.getUserOut().getAme(), secBean.getClinetBrowser(), fileName + "FILETYPE:" + fileType);
    } catch (SQLException ex) {
     Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
    }
   else
    try {
     persistentSubmission.insertToLogFileUpload("10", secBean.getClientIp(), secBean.getUser(), secBean.getUserOut().getAmoe(), secBean.getClinetBrowser(), fileName + "FILETYPE:" + fileType);
    } catch (SQLException ex) {
     Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
    }
  else if (secBean.isCheckRoleIsCommon())
   try {
    persistentSubmission.insertToLogFileUpload("8", secBean.getClientIp(), secBean.getUser(), secBean.getUserOut().getAme(), secBean.getClinetBrowser(), fileName + "FILETYPE:" + fileType);
   } catch (SQLException ex) {
    Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
   }
  else
   try {
    persistentSubmission.insertToLogFileUpload("8", secBean.getClientIp(), secBean.getUser(), secBean.getUserOut().getAmoe(), secBean.getClinetBrowser(), fileName + "FILETYPE:" + fileType);
   } catch (SQLException ex) {
    Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
   }

  if (event.getFile().getFileName() == null) {
   FacesMessage msg = new FacesMessage("Αποτυχία", "Το" + event.getFile().getFileName() + " δεν ήταν δυνατό να ανέβει.");
   FacesContext.getCurrentInstance().addMessage(null, msg);
   return;
  }

  if (fileName.equals(fileType) || fileName.contains("\\" + fileType) || fileName.contains("/" + fileType)) {
   fileName = fileName.replace(fileName, fileType);

   if (ischechFileSubmition) {
    UploadedFile uf = event.getFile();
    Boolean isExtraCheckOk = true;
    long fileSize = uf.getSize();
    
    if (fileSize > 18874368) {
     try {
      persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
     } catch (SQLException ex) {
      Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
     }
     FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Μη αποδεκτό μέγεθος αρχείου", "");
     FacesContext.getCurrentInstance().addMessage(null, msg);
     isFileSizeOk = false;
    }

    if (isFileSizeOk) {
     Process p = null;
     String isError = "false";
     MagicMatch match = null;
     //Boolean isMatch = false; //chpa
     Boolean isText = false;//chpa +++change to false

     try {
      match = Magic.getMagicMatch(uf.getContents());
      if (match.getMimeType().equals("text/plain"))
       isText = true;

     } catch (MagicMatchNotFoundException me) {
//                String extension = getExtFromFileName(fileName);
//                String fileType = extension;
//                if (fileType != null) {
//                    if (fileType.equals("txt")) {
//                        //isMatch = true;
//                    }
//                }
      isText = true;
     }

     //if (fileName.equals("csl01.txt") || fileName.equals("CSL01.txt") || fileName.equals("CSL01")) {
     // if (isText == true || isMatch == true) {
     if (isText == true) {
       
      String all = "c:\\Program Files (x86)\\F-Secure\\Anti-Virus\\fsav.exe ";
      String genFileName = secBean.getBranchCode() + secBean.getYear() + secBean.getSid() + new Date().getTime() + fileName;
      f = new File(reader.getProperty("pomUploadPath2") + genFileName);   
      f2 = new File(reader.getProperty("pomUploadPath3") + genFileName);

      try {
       is = uf.getInputstream();
       byte[] b = new byte[is.available()];
       os = new FileOutputStream(f);
       while (is.read(b) > 0)
        os.write(b);
         
      } catch (IOException ex) {
       Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
       throw new RuntimeException(ex);
      } finally {
       try {
        try {
         os.flush();
         os.close();
         is.close();
        } catch (Exception ex) {
        }

        if (reader.getProperty("pomProfileName").startsWith("LOCAL")) {
        } else if (reader.getProperty("pomProfileName").startsWith("DEV") || reader.getProperty("pomProfileName").startsWith("PROD")) {
         p = Runtime.getRuntime().exec(all + f.toString());
         BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
         String line = null;
         
         while ((line = in.readLine()) != null) {
          if (System.currentTimeMillis() - start >= 480000) {
           isError = "time";
           break;
        }
          if (line.equals("Viruses:   \t     1")) {
           isError = "virus";
           break;
          }
          //System.out.println(line);
         }
        }

        if (isError.equals("false")) {
         FileInputStream fstream = new FileInputStream(f);
          DataInputStream ins = new DataInputStream(fstream);
          BufferedReader br = new BufferedReader(new InputStreamReader(ins, "ISO-8859-7"));
         String strLine = "";
         int counterStr = 0;
         
         int rangeAscii[] = {127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142,
          143, 144, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160};
         int rangeLenbht = rangeAscii.length;

          while ((strLine = br.readLine()) != null) {
           int x = strLine.length();

           if (x > 1999) {
            //error
            isExtraCheckOk = false;
            uploadOk = false;
           try {
            persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
           } catch (SQLException ex) {
            Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
           }
          }

          counterStr++;

          for (int i = 0; i < x; i++) {
           int ascii_code = strLine.codePointAt(i);

           for (int y = 0; y < rangeLenbht; y++)
            if (ascii_code == rangeAscii[y])
             temp = 1;
         }
         }

        } else {
         try {
          persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
         } catch (SQLException ex) {
          Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
         }
         if (isError.equals("virus"))
          FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Προσοχή βρέθηκε Ιος, Μη επιτυχής Υποβολή", ""));
         else if (isError.equals("time"))
          FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Υπέρβαση χρόνου αποστολής αρχείου, Μη επιτυχής Υποβολή", ""));
         else
          FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Μη επιτυχής Υποβολή...", ""));
         return;
        }

        if (temp == 1) {
         FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Το Αρχείο δεν διαθέτει την απαιτούμενη κωδικοποίηση ISO-8859-7", "");
         FacesContext.getCurrentInstance().addMessage(null, msg);
         uploadOk = false;
         try {
          persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
         } catch (SQLException ex) {
          Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
         }
         return;
        }

        if (isExtraCheckOk == false && temp != 1) {
         try {
          persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
         } catch (SQLException ex) {
          Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
         }
         FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Λανθασμένη Δομή Αρχείου", "");
         FacesContext.getCurrentInstance().addMessage(null, msg);
         return;
        }

       } catch (IOException ex) {
        try {
         persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
        } catch (SQLException ex1) {
         Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex1);
        }
       }
      }

      //efoson den yparxoun virus, tote grafoume sthn vash k kanoume update diafora flags
      if (isError.equals("false") && isExtraCheckOk)
       if (event != null) {

        try {
         // Now you can save bytes in DB (and also content type?)
         if (selectedSubmission.getSid() != null)
          persistentUpload.insertToApd(f, f2, selectedSubmission.getSid());
         else {
          uploadOk = false;
         FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "sid is null", "");
         FacesContext.getCurrentInstance().addMessage(null, msg);
         }
        } catch (Exception ex) {
         Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
         uploadOk = false;
         FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Πρόβλημα αρχείου", "");
         FacesContext.getCurrentInstance().addMessage(null, msg);
        }

        if (uploadOk) {
         uploadRendered = false;

         FacesContext.getCurrentInstance().addMessage(null,
                 new FacesMessage(String.format("Το αρχείο '%s' ανέβηκε επιτυχώς! Επιλέξτε Επιστροφή και στη συνέχεια Έλεγχος Υποβολής", fileName)));
         return;
        }
       }

     } else {
      try {
       persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
      } catch (SQLException ex) {
       Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
      }
      FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Μη επιτρεπτός τύπος αρχείου " + match.getMimeType(), "");
      FacesContext.getCurrentInstance().addMessage(null, msg);
      return;

     }
    }
   }
  } else {
   try {
    persistentUpload.rollbackFileSubmitionFlg(selectedSubmission.getSid());
   } catch (SQLException ex) {
    Logger.getLogger(UploadBean.class.getName()).log(Level.SEVERE, null, ex);
   }
   FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(String.format("Το όνομα αρχείου πρεπει να είναι '%s'", fileType)));
   return;
  }

 }

 public static String getFileNameFromPath(String fullPath) {
  int index = fullPath.lastIndexOf("\\");
  String fileName = fullPath.substring(index + 1);

  if (fullPath.contains(".")) {
   index = fullPath.lastIndexOf(".");
   fileName = fullPath.substring(0, index);
  }
  return fileName;
 }

 private String getExtFromFileName(String fileName) {
  String ext = null;
  int li1 = fileName.lastIndexOf(".");
  if (li1 != -1)
   if (li1 + 1 < fileName.length())
    if (fileName.indexOf('/') == -1 && fileName.indexOf("\\") == -1 && fileName.indexOf("\\\\") == -1)
     ext = fileName.substring(li1 + 1, fileName.length()).toLowerCase();
    else
     ext = null;
  return ext;
 }

 public String backBtn() {
  return "submissions?faces-redirect=true";
 }

 public void performSubmissionCheck() throws SQLException {
  RequestContext context = RequestContext.getCurrentInstance();
  Boolean isValid3 = false;
  persistentUpload.updateStatus();
  isValid3 = true;
  context.addCallbackParam("isValid3", isValid3);
 }

 public Employer getSelectedSubmission() {
  return selectedSubmission;
 }

 public void setSelectedSubmission(Employer selectedSubmission) {
  this.selectedSubmission = selectedSubmission;
 }

 public Boolean getUploadRendered() {
  return uploadRendered;
 }

 public void setUploadRendered(Boolean uploadRendered) {
  this.uploadRendered = uploadRendered;
 }

 public Boolean getUploadOk() {
  return uploadOk;
 }

 public void setUploadOk(Boolean uploadOk) {
  this.uploadOk = uploadOk;
 }

 public Boolean getIsFileUpload() {
  return isFileUpload;
 }

 public void setIsFileUpload(Boolean isFileUpload) {
  this.isFileUpload = isFileUpload;
 }

 public PersistentUpload getPersistentUpload() {
  return persistentUpload;
 }

 public void setPersistentUpload(PersistentUpload persistentUpload) {
  this.persistentUpload = persistentUpload;
 }

 public SecurityBean getSecBean() {
  return secBean;
 }

 public void setSecBean(SecurityBean secBean) {
  this.secBean = secBean;
 }
}
