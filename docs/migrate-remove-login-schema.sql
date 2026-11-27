-- Ejecutar solo después de respaldar la base de datos y seleccionar el esquema correcto.
-- Elimina el vínculo de acceso de members y la tabla antigua de cuentas.

SET @schema_name = DATABASE();

SET @account_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'user_account_id'
      AND REFERENCED_TABLE_NAME = 'user_accounts'
    LIMIT 1
);
SET @drop_fk_sql = IF(
    @account_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `members` DROP FOREIGN KEY `', REPLACE(@account_fk, '`', '``'), '`')
);
PREPARE remove_account_fk FROM @drop_fk_sql;
EXECUTE remove_account_fk;
DEALLOCATE PREPARE remove_account_fk;

SET @account_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'user_account_id'
);
SET @drop_column_sql = IF(
    @account_column_exists = 0,
    'SELECT 1',
    'ALTER TABLE `members` DROP COLUMN `user_account_id`'
);
PREPARE remove_account_column FROM @drop_column_sql;
EXECUTE remove_account_column;
DEALLOCATE PREPARE remove_account_column;

DROP TABLE IF EXISTS `user_accounts`;
