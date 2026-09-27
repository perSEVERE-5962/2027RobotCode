# CAN id map

Status: scheme proposed, nothing set on hardware yet.

## Buses

| Bus | Lane | What goes on it |
|---|---|---|
| `can_s0` | A | PDH, robot level sensors, LEDs, pneumatics |
| `can_s1` | A | reserve. Shares a lane with `can_s0`, so low rate stuff only |
| `can_s2` | B, alone | swerve motors, swerve encoders, and the gyro |
| `can_s3` | C | mechanisms that reach outside the frame, like an intake |
| `can_s4` | C | mechanisms that stay inside the frame |

## The scheme

The tens digit is the subsystem, the ones digit is the job. Each bus has its own table.

`can_s0`, the robot's core:

| Decade | What |
|---|---|
| 0x | REV PDH, at its factory default of 1 |
| 1x | Range sensors |
| 2x | Floor and colour sensors |
| 3x | LEDs, pneumatics |

`can_s2`, swerve:

| Decade | What |
|---|---|
| 1x | Front left module |
| 2x | Front right module |
| 3x | Back left module |
| 4x | Back right module |
| 50 | Gyro |

In a module decade, x0 is drive, x1 is steer, and x2 is the azimuth encoder if there's a separate one.

`can_s3` and `can_s4`, game mechanisms. These are reserved, not assigned. The real ids get set at kickoff, all at once, once we know what reaches outside the frame. Both buses use the same decades.

| Decade | What |
|---|---|
| 1x | Intake |
| 2x | Feeder or indexer |
| 3x | Shooter |
| 4x | Elevator |
| 5x | Arm, wrist, gripper |
| 6x | Climber (60 to 63 only) |

## R2, the practice chassis

| Device | Type | Bus | Id | Status |
|---|---|---|---|---|
| PDH | REV PDH | can_s0 | 1 | factory default |
| Gyro | Pigeon 2 | can_s2 | 50 | planned |
| Front left drive, steer | SPARK MAX | can_s2 | 10, 11 | planned |
| Front right drive, steer | SPARK MAX | can_s2 | 20, 21 | planned |
| Back left drive, steer | SPARK MAX | can_s2 | 30, 31 | planned |
| Back right drive, steer | SPARK MAX | can_s2 | 40, 41 | planned |

## Comp bot

Krakens on Phoenix 6, CANcoders for azimuth, Pigeon 2.

| Device | Type | Bus | Id | Status |
|---|---|---|---|---|
| PDH | REV PDH | can_s0 | 1 | planned |
| Range sensors | CANrange | can_s0 | 10 to 19 | planned |
| Gyro | Pigeon 2 | can_s2 | 50 | planned |
| Front left drive, steer, encoder | TalonFX, TalonFX, CANcoder | can_s2 | 10, 11, 12 | planned |
| Front right drive, steer, encoder | same | can_s2 | 20, 21, 22 | planned |
| Back left drive, steer, encoder | same | can_s2 | 30, 31, 32 | planned |
| Back right drive, steer, encoder | same | can_s2 | 40, 41, 42 | planned |
| Game mechanisms | | can_s3, can_s4 | | at kickoff |
