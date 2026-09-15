package first.robot.subsystems.drive.io;

import org.wpilib.math.controller.PIDController;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.DCMotorSim;
import org.wpilib.system.Timer;

public class ModuleIOSim implements ModuleIO {
  private static final double DRIVE_KP = 0.0;
  private static final double DRIVE_KD = 0.0;
  private static final double DRIVE_KS = 0.0;
  private static final double DRIVE_KV_ROTATION = 0.0;
  private static final double DRIVE_KV = 0.0 / Units.rotationsToRadians(0.0 / DRIVE_KV_ROTATION);
  private static final double TURN_KP = 0.0;
  private static final double TURN_KD = 0.0;
  private static final DCMotor DRIVE_GEAR = DCMotor.getKrakenX60Foc(1);
  private static final DCMotor TURN_GEAR = DCMotor.getKrakenX60Foc(1);

  private final DCMotorSim driveSim;
  private final DCMotorSim turnSim;

  private boolean driveClosedLoop = false;
  private boolean turnClosedLoop = false;
  private PIDController driveController = new PIDController(DRIVE_KP, 0, DRIVE_KD);
  private PIDController turnController = new PIDController(TURN_KP, 0, TURN_KD);
  private double driveFFVolts = 0.0;
  private double driveAppliedVolts = 0.0;
  private double turnAppliedVolts = 0.0;

  public ModuleIOSim() {
    turnController.enableContinuousInput(-Math.PI, Math.PI);
    // Create drive and turn sim models
    driveSim =
        new DCMotorSim(
            Models.singleJointedArmFromPhysicalConstants(DRIVE_GEAR, 0.0, 0.0), DRIVE_GEAR);
    turnSim =
        new DCMotorSim(
            Models.singleJointedArmFromPhysicalConstants(TURN_GEAR, 0.0, 0.0), TURN_GEAR);

    // Wrapping for turn PID
    turnController.enableContinuousInput(-Math.PI, Math.PI);
  }

  @Override
  public void updateInputs(ModuleIOInputs inputs) {
    // Run closed-loop control
    if (driveClosedLoop) {
      driveAppliedVolts = driveFFVolts + driveController.calculate(driveSim.getAngularVelocity());
    } else {
      driveController.reset();
    }
    if (turnClosedLoop) {
      turnAppliedVolts = turnController.calculate(turnSim.getAngularPosition());
    } else {
      turnController.reset();
    }

    // Update simulation
    driveSim.setInputVoltage(Math.clamp(driveAppliedVolts, -12.0, 12.0));
    turnSim.setInputVoltage(Math.clamp(turnAppliedVolts, -12.0, 12.0));
    driveSim.update(0.05);
    turnSim.update(0.05);

    // Update drive inputs
    inputs.driveConnected = true;
    inputs.drivePositionRad = driveSim.getAngularPosition();
    inputs.driveVelocityRadPerSec = driveSim.getAngularVelocity();
    inputs.driveAppliedVolts = driveAppliedVolts;
    inputs.driveCurrentAmps = Math.abs(driveSim.getCurrentDraw());

    // Update turn inputs
    inputs.turnConnected = true;
    inputs.turnPosition = new Rotation2d(turnSim.getAngularPosition());
    inputs.turnVelocityRadPerSec = turnSim.getAngularVelocity();
    inputs.turnAppliedVolts = turnAppliedVolts;
    inputs.turnCurrentAmps = Math.abs(turnSim.getCurrentDraw());

    // Update odometry inputs
    inputs.odometryTimestamps = new double[] {Timer.getTimestamp()};
    inputs.odometryDrivePositionsRad = new double[] {inputs.drivePositionRad};
    inputs.odometryTurnPositions = new Rotation2d[] {inputs.turnPosition};
  }

  @Override
  public void setDriveOpenLoop(double output) {
    driveClosedLoop = false;
    driveAppliedVolts = output;
  }

  public void setTurnOpenLoop(double output) {
    turnClosedLoop = false;
    turnAppliedVolts = output;
  }

  @Override
  public void setDriveVelocity(double velocityRadPerSec) {
    driveClosedLoop = true;
    driveFFVolts = DRIVE_KS * Math.signum(velocityRadPerSec) + DRIVE_KV * velocityRadPerSec;
    driveController.setSetpoint(velocityRadPerSec);
  }

  @Override
  public void setTurnPosition(Rotation2d rotation) {
    turnClosedLoop = true;
    turnController.setSetpoint(rotation.getRadians());
  }
}
