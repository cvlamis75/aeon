
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intracom.persistence;

import com.intracom.bean.PropertiesReader;
import com.intracom.bean.UploadBean;
import com.intracom.model.*;
import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 *
 * @author AEON
 */
public class PersistentUpload implements Serializable {

    UserIn userIn = new UserIn();
    UserInfo userInfo = new UserInfo();
    UserOut userOut = new UserOut();
    UserShowOut userShowOut = new UserShowOut();
    User user = new User();
    private Employer selectedSubmission;
    File f;
    File f2;
    private Logger logger = Logger.getLogger("com.corejsf");
    private DataSource ds;

    public PersistentUpload() {
     try {
      try {
       PropertiesReader reader;
       reader = new PropertiesReader("properties-from-pom.properties");
       Context ctx = new InitialContext();
       ds = (DataSource) ctx.lookup("java:comp/env/" + reader.getProperty("pomDataSourceContrOps"));
      } catch (IOException ex) {
       logger.log(Level.SEVERE, null, ex);
      }
     } catch (NamingException ex) {
      logger.log(Level.SEVERE, "DataSource lookup failed", ex);
     }
    }

    public String insertToApd(File f, File f2, int submissionCodePart2) throws Exception {

        int counter = 0;
        boolean committed = false;

        Connection conn =ds.getConnection();
        PreparedStatement pstmt = null;

        DataInputStream in = null;
        OutputStream os2 = null;
        try {
            conn.setAutoCommit(false);



            FileInputStream fstream = new FileInputStream(f);
            in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-7"));
            
            os2 = new FileOutputStream(f2);
            
            String strLine;
            final int batchSize = 100;

            String query = "insert into WEB_APD_FILE_TABLE(CD, DESCR, SEQ_NUM) values(?, ?, ?)";
            pstmt = conn.prepareStatement(query); // create a statement


            while ((strLine = br.readLine()) != null && strLine.length() < 1998) {

                counter++;

                pstmt.setInt(1, counter); // set input parameter 1
                pstmt.setString(2, strLine); // set input parameter 2
                pstmt.setInt(3, submissionCodePart2); // set input parameter 3
               // pstmt.addBatch();

                pstmt.executeUpdate(); // execute insert statement
//                if (counter % batchSize == 0) {
//                    pstmt.executeBatch();
//                }

                os2.write(strLine.getBytes("ISO-8859-7"));
            }

            //pstmt.executeBatch();
            
            
        String queryStatement = "update CON_WORK_APD_EMPLOYERS SET STATE='03', FILE_NAME = ? WHERE SID = ? AND BRANCH_CODE='000' AND YEAR=9999 ";
        PreparedStatement ps2 = conn.prepareStatement(queryStatement);

        ps2.setString(1, f2.toString());
        ps2.setInt(2, submissionCodePart2);
         
        ps2.executeUpdate();
        
        
        queryStatement = "UPDATE CON_WORK_APD_EMPLOYERS SET FILE_SUBMITION_SYNCH = '2' WHERE SID = ? AND BRANCH_CODE='000' AND YEAR=9999 ";
        ps2 = conn.prepareStatement(queryStatement);

        ps2.setInt(1, submissionCodePart2);
        
        ps2.executeUpdate();


            conn.commit();
            committed = true;
            
        } catch (Exception ex) {
         Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
         if (in != null)
          try {
           in.close();
          } catch (Exception ex) {
              Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
         }
         if (os2 != null)
          try {
           os2.flush();
           os2.close();
          } catch (Exception ex) {
              Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
         }
         

            if (!committed) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }

            return null;
        }

    }

    public void updateStatus() throws SQLException {

        if (ds == null) {
            throw new SQLException("No data source");
        }
        Connection conn = ds.getConnection();
        if (conn == null) {
            throw new SQLException("No connection");
        }
        boolean committed = false;
        String query = "{call update_file_queue(?,?,?,?,?)}";

        ResultSet rs = null;
        CallableStatement cs = null;

        try {
            conn.setAutoCommit(false);
            cs = conn.prepareCall(query);

            //cs.setString(1, user.getUserAdmin_id());
            //cs.setInt(1, submissionBean.getSelectedSubmission().getContractCode());
            //+++to do check

            cs.setInt(1, selectedSubmission.getSid());
            cs.setInt(2, selectedSubmission.getYear());
            cs.setString(3, selectedSubmission.getBranchCode());

            cs.registerOutParameter(4, java.sql.Types.VARCHAR);
            cs.registerOutParameter(5, java.sql.Types.VARCHAR);

            rs = cs.executeQuery();

            userOut.setResult(cs.getString(2));
            userOut.setFailureMessage(cs.getString(3));

            //cs.execute();

            conn.commit();
            committed = true;
        } catch (SQLException ex) {
          Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (!committed) {
                conn.rollback();
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (cs != null) {
                try {
                    cs.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }

        }

    }

    public UserOut checkFileSubmition(int submissionCodePart2) throws SQLException {

        if (ds == null) {
            throw new SQLException("No data source");
        }
        Connection conn = ds.getConnection();
        if (conn == null) {
            throw new SQLException("No connection");
        }
        boolean committed = false;
        String query = "{call check_file_submition(?,?,?)}";

        ResultSet rs = null;
        CallableStatement cs = null;

        try {
            conn.setAutoCommit(false);
            cs = conn.prepareCall(query);

            cs.setInt(1, submissionCodePart2);

            cs.registerOutParameter(2, java.sql.Types.VARCHAR);
            cs.registerOutParameter(3, java.sql.Types.VARCHAR);

            rs = cs.executeQuery();

            userOut.setResult(cs.getString(2));
            userOut.setFailureMessage(cs.getString(3));

            conn.commit();
            committed = true;
        } catch (SQLException ex) {
          Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (!committed) {
                conn.rollback();
            }

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (cs != null) {
                try {
                    cs.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }

            return userOut;
        }

    }



    public void rollbackFileSubmitionFlg(int submissionCodePart2) throws SQLException {

        if (ds == null) {
            throw new SQLException("No data source");
        }
        Connection conn = ds.getConnection();
        if (conn == null) {
            throw new SQLException("No connection");
        }
        boolean committed = false;
        ResultSet rs1 = null;
        PreparedStatement ps = null;

        String queryStatement = "UPDATE CON_WORK_APD_EMPLOYERS SET FILE_SUBMITION_SYNCH = '0' WHERE SID = ? AND BRANCH_CODE='000' AND YEAR=9999 ";
        ps = conn.prepareStatement(queryStatement);

        ps.setInt(1, submissionCodePart2);


        try {
            conn.setAutoCommit(false);


            rs1 = ps.executeQuery();

            conn.commit();
            committed = true;
        } catch (SQLException ex) {
         Logger.getLogger(PersistentUpload.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (!committed) {
                conn.rollback();
            }

            if (rs1 != null) {
                try {
                    rs1.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }


        }

    }

    public DataSource getDs() {
        return ds;
    }

    public void setDs(DataSource ds) {
        this.ds = ds;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public UserOut getUserOut() {
        return userOut;
    }

    public void setUserOut(UserOut userOut) {
        this.userOut = userOut;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserShowOut getUserShowOut() {
        return userShowOut;
    }

    public void setUserShowOut(UserShowOut userShowOut) {
        this.userShowOut = userShowOut;
    }

//    public SecurityBean getSecBean() {
//        return secBean;
//    }
//
//    public void setSecBean(SecurityBean secBean) {
//        this.secBean = secBean;
//    }
    public UserIn getUserIn() {
        return userIn;
    }

    public void setUserIn(UserIn userIn) {
        this.userIn = userIn;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Employer getSelectedSubmission() {
        return selectedSubmission;
    }

    public void setSelectedSubmission(Employer selectedSubmission) {
        this.selectedSubmission = selectedSubmission;
    }
}
