package first.robot.subsystems.drive.io;

import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.DCMotorSim;
import org.wpilib.system.Timer;

public class ModuleIOSim implements ModuleIO {
  // Drive motor PID gains and feedforward constants
  private static final double DRIVE_KP = 0.0;
  private static final double DRIVE_KD = 0.0;
  private static final double DRIVE_KS = 0.0;
  private static final double DRIVE_KV_ROTATION = 0.0;
  private static final double DRIVE_KV = 0.0 / Units.rotationsToRadians(0.0 / DRIVE_KV_ROTATION);

  // Turn motor PID gains
  private static final double TURN_KP = 0.0;
  private static final double TURN_KD = 0.0;

  // Motor specifications
  private static final DCMotor DRIVE_GEAR = DCMotor.getNEO(1);
  private static final DCMotor TURN_GEAR = DCMotor.getNEO(1);

  // Motor simulation instances
  private final DCMotorSim driveSim;
  private final DCMotorSim turnSim;

  // Closed-loop control flags
  private boolean driveClosedLoop = false;
  private boolean turnClosedLoop = false;

  // PID controllers for drive and turn motors
  private PIDController driveController = new PIDController(DRIVE_KP, 0, DRIVE_KD);
  private PIDController turnController = new PIDController(TURN_KP, 0, TURN_KD);

  // Applied voltage variables
  private double driveFFVolts = 0.0;
  private double driveAppliedVolts = 0.0;
  private double turnAppliedVolts = 0.0;

  public ModuleIOSim() {
    // Enable continuous input wrapping for turn controller (-π to π)
    turnController.enableContinuousInput(-Math.PI, Math.PI);

    // Create drive and turn sim models based on gear specifications
    driveSim =
        new DCMotorSim(
            Models.singleJointedArmFromPhysicalConstants(DRIVE_GEAR, 0.0, 0.0), DRIVE_GEAR);
    turnSim =
        new DCMotorSim(
            Models.singleJointedArmFromPhysicalConstants(TURN_GEAR, 0.0, 0.0), TURN_GEAR);

    // Enable continuous input wrapping for turn PID to handle angle wraparound
    turnController.enableContinuousInput(-Math.PI, Math.PI);
  }

  @Override
  public void updateInputs(ModuleIOInputs inputs) {
    // Run closed-loop control for drive motor if enabled
    if (driveClosedLoop) {
      // Calculate output voltage with feedforward and feedback
      driveAppliedVolts = driveFFVolts + driveController.calculate(driveSim.getAngularVelocity());
    } else {
      // Reset controller if not in closed-loop mode
      driveController.reset();
    }

    // Run closed-loop control for turn motor if enabled
    if (turnClosedLoop) {
      // Calculate output voltage for turn position control
      turnAppliedVolts = turnController.calculate(turnSim.getAngularPosition());
    } else {
      // Reset controller if not in closed-loop mode
      turnController.reset();
    }

    // Update simulation with clamped voltages and step simulation
    driveSim.setInputVoltage(Math.clamp(driveAppliedVolts, -12.0, 12.0));
    turnSim.setInputVoltage(Math.clamp(turnAppliedVolts, -12.0, 12.0));
    driveSim.update(0.05);
    turnSim.update(0.05);

    // Update drive inputs from simulation state
    inputs.driveConnected = true;
    inputs.drivePositionRad = driveSim.getAngularPosition();
    inputs.driveVelocityRadPerSec = driveSim.getAngularVelocity();
    inputs.driveAppliedVolts = driveAppliedVolts;
    inputs.driveCurrentAmps = Math.abs(driveSim.getCurrentDraw());

    // Update turn inputs from simulation state
    inputs.turnConnected = true;
    inputs.turnPosition = new Rotation2d(turnSim.getAngularPosition());
    inputs.turnVelocityRadPerSec = turnSim.getAngularVelocity();
    inputs.turnAppliedVolts = turnAppliedVolts;
    inputs.turnCurrentAmps = Math.abs(turnSim.getCurrentDraw());

    // Update odometry inputs with current timestamp and positions
    inputs.odometryTimestamps = new double[] {Timer.getTimestamp()};
    inputs.odometryDrivePositionsRad = new double[] {inputs.drivePositionRad};
    inputs.odometryTurnPositions = new Rotation2d[] {inputs.turnPosition};
  }

  @Override
  public void setDriveOpenLoop(double output) {
    // Switch to open-loop control and apply voltage directly
    driveClosedLoop = false;
    driveAppliedVolts = output;
  }

  public void setTurnOpenLoop(double output) {
    // Switch to open-loop control for turn motor
    turnClosedLoop = false;
    turnAppliedVolts = output;
  }

  @Override
  public void setDriveVelocity(double velocityRadPerSec) {
    // Enable closed-loop velocity control for drive motor
    driveClosedLoop = true;
    // Calculate feedforward voltage with static friction and velocity components
    driveFFVolts = DRIVE_KS * Math.signum(velocityRadPerSec) + DRIVE_KV * velocityRadPerSec;
    // Set PID controller setpoint
    driveController.setSetpoint(velocityRadPerSec);
  }

  @Override
  public void setTurnPosition(Rotation2d rotation) {
    // Enable closed-loop position control for turn motor
    turnClosedLoop = true;
    // Set PID controller setpoint to target angle in radians
    turnController.setSetpoint(rotation.getRadians());
  }
}
