-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Εξυπηρετητής: localhost
-- Χρόνος δημιουργίας: 18 Ιαν 2026 στις 11:08:04
-- Έκδοση διακομιστή: 10.4.27-MariaDB
-- Έκδοση PHP: 8.2.0

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Βάση δεδομένων: `programma_mathimatwn`
--

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `assignments`
--

CREATE TABLE `assignments` (
  `id` bigint(20) NOT NULL,
  `active` bit(1) NOT NULL,
  `course_component` enum('LABORATORY','THEORY') NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `schedule_id` bigint(20) DEFAULT NULL,
  `teacher_id` bigint(20) NOT NULL,
  `is_general_assignment` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `assignments`
--

INSERT INTO `assignments` (`id`, `active`, `course_component`, `course_id`, `schedule_id`, `teacher_id`, `is_general_assignment`) VALUES
(23, b'1', 'THEORY', 7, 5, 1, b'0'),
(27, b'1', 'THEORY', 7, 6, 1, b'0'),
(28, b'1', 'THEORY', 15, 6, 1, b'0'),
(29, b'1', 'THEORY', 9, 6, 1, b'0'),
(36, b'1', 'THEORY', 16, 6, 2, b'0'),
(37, b'1', 'THEORY', 11, 6, 2, b'0'),
(38, b'1', 'THEORY', 13, 6, 1, b'0');

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `courses`
--

CREATE TABLE `courses` (
  `id` bigint(20) NOT NULL,
  `active` bit(1) NOT NULL,
  `capacity` int(11) NOT NULL,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `semester` int(11) NOT NULL,
  `type` enum('BASIC','ELECTIVE') NOT NULL,
  `year` int(11) DEFAULT NULL,
  `preferred_day` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') DEFAULT NULL,
  `preferred_slot` int(11) DEFAULT NULL,
  `weight` int(11) DEFAULT NULL,
  `theory_hours` int(11) DEFAULT 3,
  `lab_hours` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `courses`
--

INSERT INTO `courses` (`id`, `active`, `capacity`, `code`, `name`, `semester`, `type`, `year`, `preferred_day`, `preferred_slot`, `weight`, `theory_hours`, `lab_hours`) VALUES
(7, b'1', 150, '321-0120', 'Αγγλικά 1', 1, 'BASIC', 1, NULL, NULL, NULL, 3, 0),
(8, b'1', 150, '321-1500', 'Διακριτά Μαθηματικά Ι', 1, 'BASIC', 1, 'THURSDAY', 3, 6, 3, 0),
(9, b'1', 150, '321-1200', 'Δομημένος Προγραμματισμός', 1, 'BASIC', 1, 'MONDAY', 0, 5, 3, 0),
(10, b'1', 150, '321-1400', 'Εισαγωγή στην Επιστήμη των Υπολογιστών & Επικοινωνιών', 1, 'BASIC', 1, NULL, NULL, NULL, 3, 0),
(11, b'1', 150, '321-2000', 'Λογική Σχεδίαση', 1, 'BASIC', 1, 'MONDAY', 3, 5, 3, 0),
(12, b'1', 150, '321-1100', 'Μαθηματικά για Μηχανικούς Ι', 1, 'BASIC', 1, NULL, NULL, NULL, 3, 0),
(13, b'1', 150, '321-2400', 'Πιθανότητες και Στατιστική', 3, 'BASIC', 2, NULL, NULL, NULL, 3, 0),
(14, b'1', 150, '321-0130', 'Αγγλικά', 3, 'BASIC', 2, NULL, NULL, NULL, 3, 0),
(15, b'1', 150, '321-2100', 'Αντικειμενοστρεφής Προγραμματισμός', 3, 'BASIC', 2, 'FRIDAY', 1, 5, 3, 0),
(16, b'1', 150, '321-2450', 'Διακριτά Μαθηματικά ΙΙ', 3, 'BASIC', 2, 'THURSDAY', 1, 5, 3, 0);

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `course_schedules`
--

CREATE TABLE `course_schedules` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `end_time` varchar(255) DEFAULT NULL,
  `max_distance_km` double DEFAULT NULL,
  `max_hours_per_day` int(11) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `semester` varchar(255) NOT NULL,
  `start_time` varchar(255) DEFAULT NULL,
  `status` enum('ASSIGNMENT_PHASE','EXECUTION_PHASE','NO_SOLUTION_FOUND','REQUIREMENTS_PHASE','SOLUTION_APPROVED','SOLUTION_FOUND','TERMINATED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `year` int(11) NOT NULL,
  `active` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `course_schedules`
--

INSERT INTO `course_schedules` (`id`, `created_at`, `end_time`, `max_distance_km`, `max_hours_per_day`, `name`, `semester`, `start_time`, `status`, `updated_at`, `year`, `active`) VALUES
(1, '2025-08-17 17:15:49.000000', '21:00', 1, 9, 'Xeimerino Examino', 'Χειμερινό', '09:00', 'SOLUTION_FOUND', '2025-09-23 22:45:16.000000', 2025, b'1'),
(4, '2025-11-02 15:32:52.000000', '21:00', 1, 9, 'Χειμερινό 25-26', 'Χειμερινό', '09:00', 'SOLUTION_FOUND', '2025-11-02 17:10:47.000000', 2026, b'1'),
(5, '2025-11-29 12:03:55.000000', '21:00', 1, 9, 'test3', 'Χειμερινό', '09:00', 'SOLUTION_APPROVED', '2025-11-29 12:03:55.000000', 2025, b'1'),
(6, '2025-11-29 12:05:00.000000', '21:00', 1, 9, 'test4', 'Χειμερινό', '09:00', 'SOLUTION_APPROVED', '2026-01-18 09:54:07.000000', 2026, b'1');

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `course_teaching_hours`
--

CREATE TABLE `course_teaching_hours` (
  `course_id` bigint(20) NOT NULL,
  `component` enum('LABORATORY','THEORY') DEFAULT NULL,
  `hours` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `rooms`
--

CREATE TABLE `rooms` (
  `id` bigint(20) NOT NULL,
  `active` bit(1) NOT NULL,
  `building` varchar(255) NOT NULL,
  `capacity` int(11) NOT NULL,
  `location` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` enum('LABORATORY','TEACHING') NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `coordinate_x` double DEFAULT NULL,
  `coordinate_y` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `rooms`
--

INSERT INTO `rooms` (`id`, `active`, `building`, `capacity`, `location`, `name`, `type`, `latitude`, `longitude`, `coordinate_x`, `coordinate_y`) VALUES
(5, b'1', 'mesaio', 103, 'mesaio', 'aithousa 1', 'TEACHING', NULL, NULL, NULL, NULL),
(6, b'1', 'mesaio', 120, 'mesaio', 'aithousa 2', 'TEACHING', NULL, NULL, NULL, NULL),
(7, b'1', 'mesaio', 160, 'mesaio', 'aithousa 3', 'TEACHING', NULL, NULL, NULL, NULL),
(8, b'1', 'mesaio', 60, 'mesaio', 'aithousa 4', 'TEACHING', NULL, NULL, NULL, NULL),
(9, b'1', 'mesaio', 70, 'ΜΕΣΑΙΟ ΚΑΡΛΟΒΑΣΙ', 'aithousa 5', 'TEACHING', NULL, NULL, NULL, NULL),
(10, b'1', 'mesaio', 50, 'mesaio', 'aithousa 7', 'TEACHING', NULL, NULL, NULL, NULL),
(11, b'1', 'mesaio', 100, 'mesaio', 'aithousa 6', 'TEACHING', NULL, NULL, NULL, NULL),
(12, b'1', 'mesaio', 160, 'mesaio', 'aithousa 8', 'TEACHING', NULL, NULL, NULL, NULL),
(14, b'1', 'mesaio2', 99, 'NEO ΚΑΡΛΟΒΑΣΙ', 'aithousa 13', 'TEACHING', NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `room_availability`
--

CREATE TABLE `room_availability` (
  `id_avail` int(11) NOT NULL,
  `room_id` bigint(20) NOT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `start_time` time(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `room_availability`
--

INSERT INTO `room_availability` (`id_avail`, `room_id`, `day_of_week`, `end_time`, `start_time`) VALUES
(4, 6, 'FRIDAY', '15:00:00.000000', '12:00:00.000000'),
(5, 6, 'THURSDAY', '15:00:00.000000', '12:00:00.000000'),
(7, 6, 'FRIDAY', '18:00:00.000000', '15:00:00.000000'),
(8, 7, 'FRIDAY', '18:00:00.000000', '15:00:00.000000'),
(9, 7, 'THURSDAY', '18:00:00.000000', '15:00:00.000000'),
(10, 7, 'FRIDAY', '21:00:00.000000', '18:00:00.000000'),
(13, 8, 'FRIDAY', '15:00:00.000000', '12:00:00.000000'),
(14, 8, 'FRIDAY', '12:00:00.000000', '09:00:00.000000'),
(16, 9, 'THURSDAY', '12:00:00.000000', '09:00:00.000000'),
(17, 9, 'FRIDAY', '12:00:00.000000', '09:00:00.000000'),
(18, 9, 'FRIDAY', '18:00:00.000000', '15:00:00.000000'),
(20, 10, 'TUESDAY', '12:00:00.000000', '09:00:00.000000'),
(21, 11, 'THURSDAY', '15:00:00.000000', '12:00:00.000000'),
(22, 11, 'WEDNESDAY', '15:00:00.000000', '12:00:00.000000'),
(23, 11, 'THURSDAY', '18:00:00.000000', '15:00:00.000000'),
(24, 11, 'THURSDAY', '12:00:00.000000', '09:00:00.000000'),
(25, 11, 'WEDNESDAY', '12:00:00.000000', '09:00:00.000000'),
(33, 5, 'FRIDAY', '18:00:00.000000', '15:00:00.000000'),
(34, 5, 'FRIDAY', '21:00:00.000000', '18:00:00.000000'),
(35, 14, 'THURSDAY', '21:00:00.000000', '18:00:00.000000'),
(36, 14, 'FRIDAY', '21:00:00.000000', '18:00:00.000000'),
(37, 6, 'MONDAY', '21:00:00.000000', '18:00:00.000000');

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `schedule_results`
--

CREATE TABLE `schedule_results` (
  `id` bigint(20) NOT NULL,
  `schedule_id` bigint(20) NOT NULL COMMENT 'Αναφορά στον χρονοπρογραμματισμό',
  `assignment_id` bigint(20) NOT NULL COMMENT 'Αναφορά στην ανάθεση μαθήματος',
  `room_id` bigint(20) NOT NULL COMMENT 'Αναφορά στην αίθουσα',
  `day_of_week` varchar(20) NOT NULL COMMENT 'Ημέρα εβδομάδας (MONDAY, TUESDAY, κλπ.)',
  `start_time` time NOT NULL COMMENT 'Ώρα έναρξης μαθήματος',
  `end_time` time NOT NULL COMMENT 'Ώρα λήξης μαθήματος',
  `slot_number` int(11) NOT NULL COMMENT 'Αριθμός slot (0-3 για κάθε ημέρα)',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Αποτελέσματα χρονοπρογραμματισμού - αποθηκεύει τις λύσεις του αλγορίθμου';

--
-- Άδειασμα δεδομένων του πίνακα `schedule_results`
--

INSERT INTO `schedule_results` (`id`, `schedule_id`, `assignment_id`, `room_id`, `day_of_week`, `start_time`, `end_time`, `slot_number`, `created_at`) VALUES
(66, 6, 27, 7, 'WEDNESDAY', '09:00:00', '12:00:00', 0, '2026-01-17 22:00:20'),
(67, 6, 28, 12, 'FRIDAY', '12:00:00', '15:00:00', 1, '2026-01-17 22:00:20'),
(68, 6, 29, 7, 'MONDAY', '18:00:00', '21:00:00', 3, '2026-01-17 22:00:20'),
(69, 6, 36, 12, 'MONDAY', '18:00:00', '21:00:00', 3, '2026-01-17 22:00:20'),
(70, 6, 37, 7, 'WEDNESDAY', '18:00:00', '21:00:00', 3, '2026-01-17 22:00:20'),
(71, 6, 38, 12, 'TUESDAY', '09:00:00', '12:00:00', 0, '2026-01-17 22:00:20');

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `teacher_preferences`
--

CREATE TABLE `teacher_preferences` (
  `id` bigint(20) NOT NULL,
  `active` bit(1) NOT NULL,
  `max_capacity` int(11) DEFAULT NULL,
  `min_capacity` int(11) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `preferred_day` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') DEFAULT NULL,
  `preferred_end_time` time(6) DEFAULT NULL,
  `preferred_room_type` enum('LABORATORY','TEACHING') DEFAULT NULL,
  `preferred_start_time` time(6) DEFAULT NULL,
  `priority_weight` int(11) NOT NULL,
  `preference_type` enum('PREFERENCE','REQUIREMENT') NOT NULL,
  `assignment_id` bigint(20) NOT NULL,
  `preferred_room_id` bigint(20) DEFAULT NULL,
  `preference_weight` int(11) DEFAULT NULL,
  `preferred_slot` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `teacher_preferences`
--

INSERT INTO `teacher_preferences` (`id`, `active`, `max_capacity`, `min_capacity`, `notes`, `preferred_day`, `preferred_end_time`, `preferred_room_type`, `preferred_start_time`, `priority_weight`, `preference_type`, `assignment_id`, `preferred_room_id`, `preference_weight`, `preferred_slot`) VALUES
(12, b'1', 90, 40, NULL, 'FRIDAY', NULL, 'TEACHING', NULL, 5, 'REQUIREMENT', 28, 11, NULL, 1),
(15, b'1', 90, 50, NULL, 'WEDNESDAY', NULL, 'TEACHING', NULL, 5, 'REQUIREMENT', 27, 11, NULL, 0),
(16, b'1', 90, 50, NULL, 'MONDAY', NULL, NULL, NULL, 5, 'PREFERENCE', 36, 6, 5, 3),
(17, b'1', 50, 40, NULL, 'TUESDAY', NULL, NULL, NULL, 5, 'PREFERENCE', 36, 10, 5, 0),
(18, b'1', 60, 40, NULL, 'WEDNESDAY', NULL, 'TEACHING', NULL, 5, 'PREFERENCE', 37, 11, 5, 0),
(19, b'1', 50, 20, NULL, 'TUESDAY', NULL, 'TEACHING', NULL, 5, 'PREFERENCE', 29, 10, 5, 0),
(20, b'1', 100, 50, NULL, 'MONDAY', NULL, 'TEACHING', NULL, 5, 'PREFERENCE', 29, 6, 5, 3),
(21, b'1', 50, 30, NULL, 'TUESDAY', NULL, 'TEACHING', NULL, 5, 'PREFERENCE', 38, 10, 5, 0);

-- --------------------------------------------------------

--
-- Δομή πίνακα για τον πίνακα `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `active` bit(1) NOT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `password_reset_token` varchar(255) DEFAULT NULL,
  `password_reset_token_expiry_time` datetime(6) DEFAULT NULL,
  `role` varchar(255) NOT NULL,
  `teacher_rank` varchar(255) DEFAULT NULL,
  `teacher_type` enum('DEP','EDIP','ETEP') DEFAULT NULL,
  `username` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Άδειασμα δεδομένων του πίνακα `users`
--

INSERT INTO `users` (`id`, `active`, `email`, `full_name`, `password`, `password_reset_token`, `password_reset_token_expiry_time`, `role`, `teacher_rank`, `teacher_type`, `username`) VALUES
(1, b'1', 'kkrit@aegean.gr', 'Kuriakos Kritikos', '$2a$09$Mf9QP2dwRrB74jGvIMtOAO03D05rpqyIodGpCxLedvHwkqWiRXeO6', NULL, NULL, 'TEACHER', 'Καθηγητής', 'DEP', 'kkrit'),
(2, b'1', 'glask@aegean.gr', 'Giwrgos Laskaridiss', '$2a$10$qtPDmM.nstkMUK4kZpOvEOSJg4Ed/ij7tBhI8nbwOm6y3s9bjeUMO', NULL, NULL, 'TEACHER', 'Αναπληρωτής Καθηγητής', 'EDIP', 'glask'),
(3, b'1', 'gstergio@aegean.gr', 'Georgios Stergiopoulos', '$2a$10$E6XtSmNJhr/q4w6Fgfmc3.Jz5.c4T4uPT0GW9fuMozPd7oyT4T4h6', NULL, NULL, 'TEACHER', 'Anaplirwtis Kathigitis', 'DEP', 'gstergio'),
(4, b'1', 'admin@admin.gr', 'admin', '$2y$10$YeRqqLazD8l9EAG.b68wBOUnz3l2BL8mSs9jxIF3dY1a.vJjCAFbm', NULL, NULL, 'ADMIN', NULL, NULL, 'admin');

-- --------------------------------------------------------

--
-- Στημένη δομή για προβολή `v_schedule_results`
-- (Δείτε παρακάτω για την πραγματική προβολή)
--
CREATE TABLE `v_schedule_results` (
`id` bigint(20)
,`schedule_name` varchar(255)
,`semester` varchar(255)
,`year` int(11)
,`course_name` varchar(255)
,`course_code` varchar(255)
,`course_component` enum('LABORATORY','THEORY')
,`teacher_name` varchar(255)
,`room_name` varchar(255)
,`room_building` varchar(255)
,`day_of_week` varchar(20)
,`start_time` time
,`end_time` time
,`slot_number` int(11)
,`created_at` timestamp
);

-- --------------------------------------------------------

--
-- Δομή για προβολή `v_schedule_results`
--
DROP TABLE IF EXISTS `v_schedule_results`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_schedule_results`  AS SELECT `sr`.`id` AS `id`, `cs`.`name` AS `schedule_name`, `cs`.`semester` AS `semester`, `cs`.`year` AS `year`, `c`.`name` AS `course_name`, `c`.`code` AS `course_code`, `a`.`course_component` AS `course_component`, `u`.`full_name` AS `teacher_name`, `r`.`name` AS `room_name`, `r`.`building` AS `room_building`, `sr`.`day_of_week` AS `day_of_week`, `sr`.`start_time` AS `start_time`, `sr`.`end_time` AS `end_time`, `sr`.`slot_number` AS `slot_number`, `sr`.`created_at` AS `created_at` FROM (((((`schedule_results` `sr` join `course_schedules` `cs` on(`sr`.`schedule_id` = `cs`.`id`)) join `assignments` `a` on(`sr`.`assignment_id` = `a`.`id`)) join `courses` `c` on(`a`.`course_id` = `c`.`id`)) join `users` `u` on(`a`.`teacher_id` = `u`.`id`)) join `rooms` `r` on(`sr`.`room_id` = `r`.`id`)) ORDER BY `cs`.`id` ASC, `sr`.`day_of_week` ASC, `sr`.`slot_number` ASC  ;

--
-- Ευρετήρια για άχρηστους πίνακες
--

--
-- Ευρετήρια για πίνακα `assignments`
--
ALTER TABLE `assignments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKtayuubowb890r1dsuqkia7yke` (`course_id`,`course_component`,`schedule_id`),
  ADD KEY `FK65jjs8o9kf6nvr5thyxfkymad` (`schedule_id`),
  ADD KEY `FK67msc7a52b0l2pttoq5bhm6bk` (`teacher_id`);

--
-- Ευρετήρια για πίνακα `courses`
--
ALTER TABLE `courses`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK61og8rbqdd2y28rx2et5fdnxd` (`code`);

--
-- Ευρετήρια για πίνακα `course_schedules`
--
ALTER TABLE `course_schedules`
  ADD PRIMARY KEY (`id`);

--
-- Ευρετήρια για πίνακα `course_teaching_hours`
--
ALTER TABLE `course_teaching_hours`
  ADD KEY `FK2yupmwnld88q6h84djy7vibqe` (`course_id`);

--
-- Ευρετήρια για πίνακα `rooms`
--
ALTER TABLE `rooms`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_rooms_coordinates` (`latitude`,`longitude`),
  ADD KEY `idx_rooms_local_coordinates` (`coordinate_x`,`coordinate_y`);

--
-- Ευρετήρια για πίνακα `room_availability`
--
ALTER TABLE `room_availability`
  ADD PRIMARY KEY (`id_avail`),
  ADD KEY `FK6amy5j70qonexbd2imncdqt5w` (`room_id`);

--
-- Ευρετήρια για πίνακα `schedule_results`
--
ALTER TABLE `schedule_results`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_schedule_assignment` (`schedule_id`,`assignment_id`),
  ADD KEY `idx_schedule_results_schedule_id` (`schedule_id`),
  ADD KEY `idx_schedule_results_day_slot` (`day_of_week`,`slot_number`),
  ADD KEY `idx_schedule_results_assignment` (`assignment_id`),
  ADD KEY `idx_schedule_results_room` (`room_id`);

--
-- Ευρετήρια για πίνακα `teacher_preferences`
--
ALTER TABLE `teacher_preferences`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK1uuvlkl54iwbfws55u1e8qrbr` (`assignment_id`),
  ADD KEY `FK2ox9mpjr8b851be2j6148dhha` (`preferred_room_id`);

--
-- Ευρετήρια για πίνακα `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  ADD UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`);

--
-- AUTO_INCREMENT για άχρηστους πίνακες
--

--
-- AUTO_INCREMENT για πίνακα `assignments`
--
ALTER TABLE `assignments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=39;

--
-- AUTO_INCREMENT για πίνακα `courses`
--
ALTER TABLE `courses`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT για πίνακα `course_schedules`
--
ALTER TABLE `course_schedules`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT για πίνακα `rooms`
--
ALTER TABLE `rooms`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT για πίνακα `room_availability`
--
ALTER TABLE `room_availability`
  MODIFY `id_avail` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

--
-- AUTO_INCREMENT για πίνακα `schedule_results`
--
ALTER TABLE `schedule_results`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=72;

--
-- AUTO_INCREMENT για πίνακα `teacher_preferences`
--
ALTER TABLE `teacher_preferences`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

--
-- AUTO_INCREMENT για πίνακα `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Περιορισμοί για άχρηστους πίνακες
--

--
-- Περιορισμοί για πίνακα `assignments`
--
ALTER TABLE `assignments`
  ADD CONSTRAINT `FK65jjs8o9kf6nvr5thyxfkymad` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedules` (`id`),
  ADD CONSTRAINT `FK67msc7a52b0l2pttoq5bhm6bk` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK6p1m72jobsvmrrn4bpj4168mg` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`);

--
-- Περιορισμοί για πίνακα `course_teaching_hours`
--
ALTER TABLE `course_teaching_hours`
  ADD CONSTRAINT `FK2yupmwnld88q6h84djy7vibqe` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`);

--
-- Περιορισμοί για πίνακα `room_availability`
--
ALTER TABLE `room_availability`
  ADD CONSTRAINT `FK6amy5j70qonexbd2imncdqt5w` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Περιορισμοί για πίνακα `schedule_results`
--
ALTER TABLE `schedule_results`
  ADD CONSTRAINT `fk_schedule_results_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_schedule_results_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  ADD CONSTRAINT `fk_schedule_results_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedules` (`id`) ON DELETE CASCADE;

--
-- Περιορισμοί για πίνακα `teacher_preferences`
--
ALTER TABLE `teacher_preferences`
  ADD CONSTRAINT `FK1uuvlkl54iwbfws55u1e8qrbr` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  ADD CONSTRAINT `FK2ox9mpjr8b851be2j6148dhha` FOREIGN KEY (`preferred_room_id`) REFERENCES `rooms` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
