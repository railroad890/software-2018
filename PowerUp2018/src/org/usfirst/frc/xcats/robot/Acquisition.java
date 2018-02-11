package org.usfirst.frc.xcats.robot;

import org.usfirst.frc.xcats.robot.XCatsSpeedController.SCType;

import edu.wpi.first.wpilibj.DoubleSolenoid;

public class Acquisition {
	
	private XCatsSpeedController _leftAcquisition;
	private XCatsSpeedController _rightAcquisition;
	private DoubleSolenoid _armsSolenoid;
	
	public Acquisition() {
		_leftAcquisition = new XCatsSpeedController("Left Acquisition", Enums.LEFT_ACQUISITION_CAN_ID, true, SCType.TALON, null, null);
		_rightAcquisition = new XCatsSpeedController("Right Acquisition", Enums.RIGHT_ACQUISITION_CAN_ID, true, SCType.TALON, null, null);
		_rightAcquisition.setFollower(Enums.LEFT_ACQUISITION_CAN_ID);
		_rightAcquisition.setInverted(true);
		
		_armsSolenoid = new DoubleSolenoid(Enums.PCM_CAN_ID, Enums.PCM_ARMS_IN, Enums.PCM_ARMS_OUT);
	}
	
	//intake cube
	public void intake() {
		_leftAcquisition.set(Enums.ACQUISITION_SPEED);
	}
	
	//push out cube
	public void release() {
		_leftAcquisition.set(-Enums.ACQUISITION_SPEED);
	}
	
	public void stop() {
		_leftAcquisition.set(0);
	}
	
	//push secondary wheels for acquisition in
	public void armsIn() {
		_armsSolenoid.set(DoubleSolenoid.Value.kForward);
	}
	
	//pull secondary wheels for acquisition out
	public void armsOut() {
		_armsSolenoid.set(DoubleSolenoid.Value.kReverse);	
	}
	
	public void raiseLinkage() {
		
	}
	
	public void lowerLinkage() {
		
	}
	
	public void stopLinkage() {
		
	}

}
