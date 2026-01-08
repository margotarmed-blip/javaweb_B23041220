/*
SQLyog Community v13.3.0 (64 bit)
MySQL - 8.3.0 : Database - code
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`code` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `code`;

/*Table structure for table `t_blob` */

DROP TABLE IF EXISTS `t_blob`;

CREATE TABLE `t_blob` (
  `id` char(64) NOT NULL,
  `content` longblob NOT NULL,
  `file_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`,`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `t_commit` */

DROP TABLE IF EXISTS `t_commit`;

CREATE TABLE `t_commit` (
  `id` char(64) NOT NULL,
  `tree_id` char(64) NOT NULL,
  `parent_id` char(64) NOT NULL,
  `owner_id` int NOT NULL,
  `time` timestamp NULL DEFAULT NULL,
  `message` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `t_repo` */

DROP TABLE IF EXISTS `t_repo`;

CREATE TABLE `t_repo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `owner` int NOT NULL,
  `is_public` tinyint(1) NOT NULL DEFAULT '1',
  `commit_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `t_tree` */

DROP TABLE IF EXISTS `t_tree`;

CREATE TABLE `t_tree` (
  `id` char(64) NOT NULL,
  `tree_blob` longtext NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `t_user` */

DROP TABLE IF EXISTS `t_user`;

CREATE TABLE `t_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `pwd_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `permission` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/* Procedure structure for procedure `sp_commit_version` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_commit_version` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_commit_version`(
    IN p_repo_id INT,
    IN p_user_id INT,
    IN p_message TEXT,
    IN p_tree_id CHAR(64),
    IN p_tree_blob LONGTEXT,
    IN p_new_commit_id CHAR(64),
    OUT p_res BOOLEAN
)
BEGIN
    DECLARE v_parent_id CHAR(64);
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
        ROLLBACK;
        SET p_res = FALSE;
    END;

    START TRANSACTION;
        IF EXISTS (SELECT 1 FROM t_repo WHERE id = p_repo_id AND owner = p_user_id) THEN
            INSERT IGNORE INTO t_tree (id, tree_blob) VALUES (p_tree_id, p_tree_blob);
            SELECT commit_id INTO v_parent_id FROM t_repo WHERE id = p_repo_id;
            INSERT INTO t_commit (id, tree_id, parent_id, owner_id, time, message) 
            VALUES (p_new_commit_id, p_tree_id, IFNULL(v_parent_id, ''), p_user_id, NOW(), p_message);
            UPDATE t_repo SET commit_id = p_new_commit_id WHERE id = p_repo_id;     
            COMMIT;
            SET p_res = TRUE;
        ELSE
            ROLLBACK;
            SET p_res = FALSE;
        END IF;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_create_repo` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_create_repo` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_create_repo`(
    IN p_name VARCHAR(64),
    IN p_owner INT,
    IN p_is_public TINYINT
)
BEGIN
    INSERT INTO t_repo (name, owner, is_public) VALUES (p_name, p_owner, p_is_public);
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_delete_repo` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_delete_repo` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_delete_repo`(
    IN p_repo_id INT,
    IN p_user_id INT
)
BEGIN
    DELETE FROM t_repo WHERE id = p_repo_id AND owner = p_user_id;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_delete_user` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_delete_user` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_delete_user`(
    IN p_target_user_id INT
)
BEGIN
    DELETE FROM t_repo WHERE owner = p_target_user_id;
    DELETE FROM t_user WHERE id = p_target_user_id;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_fork_repo` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_fork_repo` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_fork_repo`(
    IN p_repo_id INT,
    IN p_user_id INT
)
BEGIN
    DECLARE v_name VARCHAR(64);
    DECLARE v_commit_id CHAR(64);
    DECLARE v_is_public TINYINT(1);
    DECLARE v_exists INT;

    SELECT name, commit_id, is_public INTO v_name, v_commit_id, v_is_public
    FROM t_repo WHERE id = p_repo_id;

    SELECT COUNT(*) INTO v_exists FROM t_repo 
    WHERE owner = p_user_id AND name = v_name;

    IF v_exists > 0 THEN
        SET v_name = CONCAT(v_name, '-fork');
    END IF;

    INSERT INTO t_repo (name, owner, is_public, commit_id)
    VALUES (v_name, p_user_id, v_is_public, v_commit_id);

END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_get_blob` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_get_blob` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_blob`(
    IN p_filename VARCHAR(255),
    IN p_hash CHAR(64)
)
BEGIN
    SELECT content FROM t_blob WHERE id = p_hash AND file_name = p_filename;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_get_latest_tree` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_get_latest_tree` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_latest_tree`(
    IN p_repo_id INT,
    IN p_user_id INT
)
BEGIN
    SELECT t.tree_blob 
    FROM t_tree t 
    JOIN t_commit c ON t.id = c.tree_id 
    JOIN t_repo r ON c.id = r.commit_id 
    JOIN t_user u ON u.id = p_user_id
    WHERE r.id = p_repo_id 
      AND (r.is_public = 1 OR r.owner = p_user_id OR u.permission > 0);
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_get_repos` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_get_repos` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_repos`(
    IN p_user_id INT
)
BEGIN
    SELECT id, name, is_public, commit_id 
    FROM t_repo 
    WHERE owner = p_user_id; 
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_login_user` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_login_user` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_login_user`(
    IN p_name VARCHAR(32), 
    IN p_pwd_hash VARCHAR(128)
)
BEGIN
    SELECT id, name, pwd_hash, permission 
    FROM t_user 
    WHERE name = p_name AND pwd_hash = p_pwd_hash;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_register_user` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_register_user` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_register_user`(
    IN p_name VARCHAR(32),
    IN p_pwd_hash VARCHAR(128),
    OUT p_result INT
)
BEGIN
    IF EXISTS (SELECT 1 FROM t_user WHERE name = p_name) THEN
        SET p_result = 1;
    ELSE
        INSERT INTO t_user (name, pwd_hash) VALUES (p_name, p_pwd_hash);
        SET p_result = 0;
    END IF;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_update_repo_visibility` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_update_repo_visibility` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_update_repo_visibility`(
    IN p_repo_id INT,
    IN p_user_id INT,
    IN p_is_public TINYINT
)
BEGIN
    UPDATE t_repo 
    SET is_public = p_is_public 
    WHERE id = p_repo_id AND owner = p_user_id;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_update_user_permission` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_update_user_permission` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_update_user_permission`(
    IN p_target_user_id INT,
    IN p_new_permission INT
)
BEGIN
    UPDATE t_user SET permission = p_new_permission WHERE id = p_target_user_id;
END */$$
DELIMITER ;

/* Procedure structure for procedure `sp_upload_blob` */

/*!50003 DROP PROCEDURE IF EXISTS  `sp_upload_blob` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_upload_blob`(
    IN p_id CHAR(64),
    IN p_file_name VARCHAR(255),
    IN p_content LONGBLOB
)
BEGIN
    INSERT IGNORE INTO t_blob (id, file_name, content) VALUES (p_id, p_file_name, p_content);
END */$$
DELIMITER ;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
