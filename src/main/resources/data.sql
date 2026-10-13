-- Seed data for development

-- Roles
INSERT INTO app_role (id, name, description) VALUES (1, 'Admin', 'Administrator with full access');
INSERT INTO app_role (id, name, description) VALUES (2, 'User', 'Standard user');

-- Admin user (plain text password for dev - use hashed in production)
INSERT INTO app_user (id, user_name, first_name, last_name, email, password, password_salt)
VALUES (1, 'admin', 'Admin', 'User', 'admin@localhost', 'admin', NULL);

-- Assign Admin role to admin user
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);

-- Main menu items
INSERT INTO main_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (1, 1, 'Dashboard', '/', 'Dashboard', NULL, NULL);

INSERT INTO main_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (2, 2, 'Example CRUD', '/example/crud', 'TableChart', NULL, NULL);

-- Admin menu items
INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (1, 1, 'Administration', NULL, 'AdminPanelSettings', NULL, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (2, 1, 'Users', '/admin/users', 'People', 1, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (3, 2, 'Roles', '/admin/roles', 'Security', 1, 'Admin');

-- CMS Admin menu items
INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (4, 3, 'CMS', NULL, 'Article', NULL, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (5, 1, 'Site Settings', '/admin/cms/settings', 'Settings', 4, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (6, 2, 'Menus', '/admin/cms/menus', 'Menu', 4, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (7, 3, 'Templates', '/admin/cms/templates', 'ViewQuilt', 4, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (8, 4, 'Pages', '/admin/cms/pages', 'Description', 4, 'Admin');

INSERT INTO admin_menu_item (id, item_order, name, link, icon, parent_id, roles)
VALUES (9, 5, 'Submissions', '/admin/cms/submissions', 'Inbox', 4, 'Admin');

-- Reset identity sequences past the seeded IDs
ALTER TABLE app_role ALTER COLUMN id RESTART WITH 100;
ALTER TABLE app_user ALTER COLUMN id RESTART WITH 100;
ALTER TABLE main_menu_item ALTER COLUMN id RESTART WITH 100;
ALTER TABLE admin_menu_item ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cms_template ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cms_page ALTER COLUMN id RESTART WITH 100;

-- Example CRUD table with audit fields
CREATE TABLE IF NOT EXISTS example_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(50) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Sample data for example table
INSERT INTO example_table (name, description, status, created_by, updated_by)
VALUES ('Sample Item 1', 'This is a sample item for testing', 'Active', 'admin@localhost', 'admin@localhost');

INSERT INTO example_table (name, description, status, created_by, updated_by)
VALUES ('Sample Item 2', 'Another sample item', 'Active', 'admin@localhost', 'admin@localhost');

-- Default site settings
INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('site_name', 'Dashboard', 'text', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('site_logo', '/logo.svg', 'image', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('site_logo_width', '100', 'number', 'admin@localhost', 'admin@localhost');

-- Footer settings
INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('footer_copyright', '© 2026 My Company. All rights reserved.', 'text', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('footer_text', '', 'text', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('footer_bg_color', '#1a1a2e', 'text', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('footer_text_color', '#ffffff', 'text', 'admin@localhost', 'admin@localhost');

INSERT INTO site_setting (setting_key, setting_value, setting_type, created_by, updated_by)
VALUES ('footer_links', '[]', 'json', 'admin@localhost', 'admin@localhost');

-- Starter CMS templates
INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (1, 'Basic Page', 'A simple page with a title and body content',
'{"sections":[{"key":"main","label":"Main Content","blocks":[{"key":"title","type":"text","label":"Page Title","required":true},{"key":"body","type":"richtext","label":"Body Content","required":true}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (2, 'Hero Page', 'A page with hero section and body content',
'{"sections":[{"key":"hero","label":"Hero Section","blocks":[{"key":"heading","type":"text","label":"Heading","required":true},{"key":"subheading","type":"text","label":"Subheading","required":false},{"key":"background_image","type":"image","label":"Background Image","required":false}]},{"key":"body","label":"Body Content","blocks":[{"key":"content","type":"richtext","label":"Main Content","required":true}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (3, 'Data Grid Page', 'A page that displays data from a database table in a grid',
'{"sections":[{"key":"header","label":"Page Header","blocks":[{"key":"title","type":"text","label":"Page Title","required":true},{"key":"description","type":"richtext","label":"Description","required":false}]},{"key":"grid","label":"Data Grid","blocks":[{"key":"config","type":"datagrid","label":"Grid Configuration","required":true}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (4, 'Landing Page', 'A page with hero banner, features, and contact form',
'{"sections":[{"key":"hero","label":"Hero Banner","blocks":[{"key":"banner","type":"hero","label":"Hero Banner","required":true}]},{"key":"features","label":"Features","blocks":[{"key":"title","type":"text","label":"Section Title","required":false},{"key":"items","type":"repeater","label":"Feature Items","required":false,"blockSchema":[{"key":"title","type":"text","label":"Title"},{"key":"description","type":"richtext","label":"Description"},{"key":"icon","type":"image","label":"Icon"}]}]},{"key":"contact","label":"Contact","blocks":[{"key":"heading","type":"text","label":"Section Heading","required":false},{"key":"form","type":"contactform","label":"Contact Form","required":true}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (5, 'Staff Directory', 'A page for listing team/staff profiles',
'{"sections":[{"key":"header","label":"Page Header","blocks":[{"key":"title","type":"text","label":"Page Title","required":true},{"key":"intro","type":"richtext","label":"Introduction","required":false}]},{"key":"staff","label":"Staff Profiles","blocks":[{"key":"members","type":"repeater","label":"Team Members","required":true,"blockSchema":[{"key":"photo","type":"image","label":"Photo"},{"key":"name","type":"text","label":"Name"},{"key":"bio","type":"richtext","label":"Biography"}]}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (6, 'Contact Page', 'A contact page with info and form',
'{"sections":[{"key":"header","label":"Page Header","blocks":[{"key":"title","type":"text","label":"Page Title","required":true},{"key":"info","type":"richtext","label":"Contact Information","required":false}]},{"key":"form","label":"Contact Form","blocks":[{"key":"form","type":"contactform","label":"Contact Form","required":true}]},{"key":"map","label":"Map","blocks":[{"key":"embed","type":"html","label":"Map Embed Code","required":false}]}]}',
true, 'admin@localhost', 'admin@localhost');

INSERT INTO cms_template (id, name, description, structure, is_active, created_by, updated_by)
VALUES (7, 'Registration Page', 'A page with user registration form',
'{"sections":[{"key":"header","label":"Page Header","blocks":[{"key":"title","type":"text","label":"Page Title","required":true},{"key":"intro","type":"richtext","label":"Introduction","required":false}]},{"key":"registration","label":"Registration","blocks":[{"key":"form","type":"registrationform","label":"Registration Form","required":true}]}]}',
true, 'admin@localhost', 'admin@localhost');

-- Sample CMS page
INSERT INTO cms_page (id, title, slug, template_id, content, status, meta_title, meta_description, created_by, updated_by)
VALUES (1, 'About Us', 'about-us', 1,
'{"main.title":"About Us","main.body":"<h2>Welcome</h2><p>This is a sample CMS page created with the Basic Page template. Edit this content from the admin panel.</p>"}',
'published', 'About Us - Dashboard', 'Learn more about us', 'admin@localhost', 'admin@localhost');
