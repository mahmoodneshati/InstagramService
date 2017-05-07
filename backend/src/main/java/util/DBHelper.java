package util;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import entity.InstagramUser;


import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;


/**
 * Created by neshati on 1/24/2017.
 * Behpardaz
 */
public class DBHelper {

    private static  int MIN_POOL_SIZE = 5;
    private static  int Acquire_Increment = 5;
    private static  int MAX_POOL_SIZE = 20;
    ComboPooledDataSource cpds = new ComboPooledDataSource();
    Properties prop = new Properties();



    private String user;// = "sa";
    private String password;//
    private String dbName;// = "BPJ_SDP_MS_Currency";
    private String host;// = "172.16.4.199";
    private String port;// = "1433";
    private String driverName;// = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private String connectionString;// = "jdbc:sqlserver://" +
            //host +
            //"\\SQLEXPRESS:" +
            //port +
            //";databaseName=" +
            //dbName +
            //";" ;
/*
            +
            "user=" +
            user +
            ";" +
            "password=" +
            password;
*/

    private void init(){
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream input = loader.getResourceAsStream("config.properties");
            prop.load(input);
            MIN_POOL_SIZE = Integer.parseInt(prop.getProperty("MIN_POOL_SIZE"));
            Acquire_Increment = Integer.parseInt(prop.getProperty("Acquire_Increment"));
            MAX_POOL_SIZE = Integer.parseInt(prop.getProperty("MAX_POOL_SIZE"));
            user= prop.getProperty("user");
            password= prop.getProperty("password");
            dbName = prop.getProperty("dbName");
            host = prop.getProperty("host");
            port = prop.getProperty("port");
            driverName = prop.getProperty("driverName");

            connectionString = "jdbc:sqlserver://" +
                    host +
                    "\\SQLEXPRESS:" +
                    port +
                    ";databaseName=" +
                    dbName +
                    ";" ;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initConnectionPooling(){
        try {
            cpds.setDriverClass(driverName); //loads the jdbc driver
            cpds.setJdbcUrl(connectionString);
            cpds.setUser(user);
            cpds.setPassword(password);
            cpds.setMinPoolSize(MIN_POOL_SIZE);
            cpds.setAcquireIncrement(Acquire_Increment);
            cpds.setMaxPoolSize(MAX_POOL_SIZE);
        }
        catch (PropertyVetoException e) {
            e.printStackTrace();
        }

    }
    private Connection getConnection() {
        try {

            return cpds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static DBHelper dbHelper;

    public static DBHelper getInstance() {
        if (dbHelper == null) {
            dbHelper = new DBHelper();
            dbHelper.init();
            dbHelper.initConnectionPooling();
        }
        return dbHelper;
    }

    private DBHelper() {
    }


    public void updateFollowersTable( ArrayList<InstagramUser> followersEntities,String mainID) {
        Connection conn = getConnection();
        int sum=0;
        for (InstagramUser nextFollower : followersEntities) {
            insertNewUser(conn, nextFollower);
            sum+= insertNewFollower(conn,nextFollower,mainID);
        }
        System.out.println("In total " + sum + " followers are added for user "+ mainID);
    }

    private int insertNewUser(Connection conn, InstagramUser nextFollower) {
        try {
            PreparedStatement statement
                    = conn.prepareStatement("INSERT INTO [user] VALUES(?,?,?,?) where not EXISTS (SELECT " +
                    "* from [USER] where user.id = ? )");
            Calendar cal = Calendar.getInstance();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(cal.getTimeInMillis());
            statement.setString(1, nextFollower.getUserID());
            statement.setString(2, nextFollower.getUsername());
            statement.setString(3, nextFollower.getUserFullName());
            statement.setTimestamp(4, timestamp);
            statement.setString(5, nextFollower.getUserID());
            int result = statement.executeUpdate();
            statement.close();
            return result;
        } catch (Exception e) {
            System.err.println(nextFollower.getUserID() +"\t"+nextFollower.getUserFullName() +" is already in DB!");
        }
        return 0;
    }
    private int     insertNewFollower(Connection conn, InstagramUser nextFollower, String mainID) {
        try {
            PreparedStatement statement
                    = conn.prepareStatement("INSERT INTO follow VALUES(?,?,?)");
            Calendar cal = Calendar.getInstance();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(cal.getTimeInMillis());
            statement.setString(1, nextFollower.getUserID());
            statement.setString(2, mainID);
            statement.setTimestamp(3, timestamp);
            int result = statement.executeUpdate();
            statement.close();
            return result;
        } catch (Exception e) {
            try {
                assert conn != null;
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }


    public ArrayList<InstagramUser> selectFollowers(String mainID) {
        String sql = "SELECT        id, username, fullname " +
                "FROM            follow INNER JOIN [user] ON follow.followSource = id\n" +
                "WHERE        follow.followDestination = ?";
        Connection connection = getConnection();

        ArrayList<InstagramUser> list = new ArrayList<InstagramUser>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, mainID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String userID = resultSet.getString("id");
                String username = resultSet.getString("username");
                String fullname = resultSet.getString("fullname");
                InstagramUser iu = new InstagramUser(username,userID,fullname,null);
                list.add(iu);
            }
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deleteFollowers(ArrayList<InstagramUser> instagramUsers, String userID) {
        Connection conn = getConnection();
        int sum = 0 ;
        for (InstagramUser nextFollower : instagramUsers) {
            sum+= deleteFollower(conn, nextFollower, userID);
        }
        System.out.println("In total " + sum + " followers are removed for user "+ userID);
    }

    private int deleteFollower(Connection conn, InstagramUser nextFollower, String userID) {
        try {
            PreparedStatement statement
                    = conn.prepareStatement("delete from follow where followDestination = ? and followSource = ?");
            statement.setString(1, userID);
            statement.setString(2, nextFollower.getUserID());
            int result = statement.executeUpdate();
            statement.close();
            return result;
        } catch (Exception e) {
            try {
                assert conn != null;
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return 0;
    }

    public void insertNewUser(String instagram_userID, String instagram_token, String serviceName) {
        Connection conn = getConnection();
        try {
            PreparedStatement statement
                    = conn.prepareStatement("INSERT INTO registeredUsers VALUES(?,?,?,?)");
            Calendar cal = Calendar.getInstance();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(cal.getTimeInMillis());
            statement.setString(1, instagram_userID);
            statement.setString(2, serviceName);
            statement.setString(3, instagram_token);
            statement.setTimestamp(4, timestamp);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            try {
                assert conn != null;
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
