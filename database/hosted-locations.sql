-- Safe starter locations for a new hosted database; these do not overwrite existing rows.
INSERT IGNORE INTO locations (location_name, building) VALUES
('Central Library 2nd Floor Reading Room','Main Library'),
('Science Block Chemistry Lab 201','Science Wing B'),
('Engineering Auditorium Main Hall','Tech Complex'),
('Student Cafeteria Ground Floor','Student Activity Center'),
('Sports Complex Gymnasium','Athletic Arena'),
('Computer Science Lab 3','IT & Software Block'),
('Administrative Office Lobby','Central Admin Building');
