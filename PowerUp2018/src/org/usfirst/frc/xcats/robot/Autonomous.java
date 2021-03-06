package org.usfirst.frc.xcats.robot;
//package org.usfirst.frc.team191.robot;
//package org.usfirst.frc.team191.robot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Autonomous {

	private RobotControls _controls;
	private ArrayList<AutonomousStep> _steps;

	private int _currentStep=0;
	private float _initialYaw = 0;
	private float _initCompassHeading=0;
	private boolean _angleHasBeenCalculated = false;
	private double _calculatedAngle = 0;
	private boolean _hasCaptured=false;

	private double _totalAutoTime = Enums.AUTONOMOUS_TIME;
	private AutonomousStep _currentAutoStep;
	private Timer _stepTimer = new Timer();
	private Timer _autoTimer = new Timer();

	private boolean _isExecuting = false;
	private boolean _cancelExecution = false;

	//private DigitalOutput _lightsColor = new DigitalOutput(Enums.LIGHTS_ALLIANCE_COLOR);//digial output for controlling color of lights

	private Acquisition _acquisition;
	private Elevator _elevator;

	private final String _defaultAuto = "Do Nothing";
	private final String _autoForwardOnly = "Go forward only and stop";
	private final String _driveForward = "Cross Line";
	private final String _autoL1 = "L1";
	private final String _autoL2 = "L2";
	private final String _autoCenter = "C";
	private final String _autoR1 = "R1";
	private final String _autoR2 = "R2";
	private final String _autoReadFile = "TextFile read";
	private final String _autoTestSpeed = "Run test time at input speed";
	private final String _autoTestTime = "Test time";
	private final String _autoTestGear = "Test High Gear";
	private final String _autoTestDistance = "Run for 60 in at input speed";
	private final String _autoInTeleop = "TeleopCommands";
	private final String _autoRotator = "Test Rotations";

	//Sendable chooser strings for overide to scale
	private final String _autoCrossCourtYes = "Yes"; //override to score on opposite side of scale
	private final String _autoCrossCourtNo = "No"; //don't override to score on opposite side of scale


	//sendable chooser for scoring preference
	private final String _scoringPreferenceSwitch = "Switch";
	private final String _scoringPreferenceScale = "Scale";
	private Navx _navx;

	private String _autoSelected;
	private String _crossCourtSelected;
	private String _gameData;
	private String _scoringPreference;
	private char _startSide = 'L';
	private char _notStartSide = 'R';
	private double _angleMod1 = 1;
	private double _angleMod2 = -1;
	private char _scoringSide = 'R';
	private SendableChooser _robotPosition;
	private SendableChooser _crossCourt;
	private SendableChooser _scoringPreferences;

	private boolean _gameDataFailed = false;
	private Timer _dataTimer;


	public Autonomous (RobotControls controls)
	{

		_controls = controls;      	//passes the controls object reference

		_robotPosition = new SendableChooser();
		_robotPosition.addDefault("Default Auto", _defaultAuto);
		_robotPosition.addObject(_autoForwardOnly, _autoForwardOnly);
		_robotPosition.addObject(_autoL1, _autoL1);
		_robotPosition.addObject(_autoR1, _autoR1);
		_robotPosition.addObject(_autoCenter, _autoCenter);
		_robotPosition.addObject(_autoReadFile,_autoReadFile);
		_robotPosition.addObject(_autoTestSpeed, _autoTestSpeed);
		_robotPosition.addObject(_autoTestDistance,_autoTestDistance);
		_robotPosition.addObject(_autoRotator, _autoRotator);
		SmartDashboard.putData("Auto choices", _robotPosition);	

		_crossCourt = new SendableChooser();
		_crossCourt.addDefault(_autoCrossCourtNo, _autoCrossCourtNo);
		_crossCourt.addObject(_autoCrossCourtYes, _autoCrossCourtYes);
		SmartDashboard.putData("Cross Court Choices", _crossCourt);	

		_scoringPreferences = new SendableChooser();
		_scoringPreferences.addDefault(_scoringPreferenceSwitch, _scoringPreferenceSwitch);
		_scoringPreferences.addObject(_scoringPreferenceScale, _scoringPreferenceScale);
		SmartDashboard.putData("Scoring Preference", _scoringPreferences);	



		/**
		 * This autonomous (along with the chooser code above) shows how to select between different autonomous modes
		 * using the dashboard. The sendable chooser code works with the Java SmartDashboard. If you prefer the LabVIEW
		 * Dashboard, remove all of the chooser code and uncomment the getString line to get the auto name from the text box
		 * below the Gyro
		 *
		 * You can add additional auto modes by adding additional comparisons to the switch structure below with additional strings.
		 * If using the SendableChooser make sure to add them to the chooser code above as well.
		 */

		SmartDashboard.putNumber(_autoTestSpeed, 0.5); 			//this is the speed to run the auto calibration test
		//put any properties here on the smart dashboard that you want to adjust from there.
		SmartDashboard.putNumber(_autoTestTime, 3);
		SmartDashboard.putBoolean(_autoTestGear, false);

		System.out.println("auto constructor");

	}

	public Autonomous (RobotControls controls, ArrayList<AutonomousStep> mysteps, double totalTime){
		// this "autonomous" instantiation can be called from teleop to do a set of steps
		_autoSelected = this._autoInTeleop;
		_controls = controls;
		_steps = mysteps;
		_totalAutoTime = totalTime;
		init();	
	}

	public void setSteps(ArrayList<AutonomousStep> mysteps){
		_steps = mysteps;
		init();

	}
	public void init ()
	{
		System.out.println("auto init");

		if (_autoSelected != this._autoInTeleop){
			_autoSelected = (String) _robotPosition.getSelected();
			System.out.println("Auto selected: " + _autoSelected);

			_crossCourtSelected = (String) _crossCourt.getSelected();
			System.out.println("Cross Court Selected: " + _crossCourtSelected);

			_scoringPreference = (String) _scoringPreferences.getSelected();
			System.out.println("Scoring Preference: " + _scoringPreference);

			_gameData = DriverStation.getInstance().getGameSpecificMessage();
			System.out.println(_gameData);

			if(this._gameData.length() < 2){
				System.out.println("Game data failed on first try, initiating loop");
				this._dataTimer.start();
				this._gameDataFailed = true;
			}else {
				//build the steps for the selected autonomous
				setAuto();
			}
		}

		this._acquisition = this._controls.getAcquisition();
		this._elevator = this._controls.getElevator();
		_navx = _controls.getNavx();
		_navx.zeroYaw();
		_initCompassHeading = _navx.getCompassHeading();
		_controls.getDrive().zeroEncoder();
		_initialYaw = _navx.getYaw();
		_currentStep = 0;
		_angleHasBeenCalculated =false;
		_currentAutoStep = null;
		_autoTimer.start();
		_stepTimer.start();
		_isExecuting = false;
		_cancelExecution = false;
		_hasCaptured=false;
		this.updateStatus();


	}

	public void disable(){
		_steps = null;

		updateStatus();

	}
	private void setAuto ()
	{	

		//we are going to construct the steps needed for our autonomous mode
		//		int choice=1;
		double speedTest = SmartDashboard.getNumber(_autoTestSpeed, 0.5);
		int calibrationRunTime = (int) SmartDashboard.getNumber(_autoTestTime, 2);
		System.out.println("Calibration run time: "+calibrationRunTime);
		System.out.println("speedTest: "+speedTest);
		boolean highSpeedTest = SmartDashboard.getBoolean(_autoTestGear, false);
		String caseName="";
		_steps =  new ArrayList<AutonomousStep>();

		boolean blueAlliance = false;





		//		if (DriverStation.getInstance().getAlliance() == DriverStation.Alliance.Blue){
		//			blueAlliance = true;
		//			_lightsColor.set(false);//sets lights to match alliance color
		//		}else
		//			_lightsColor.set(true);//sets lights to match alliance color

		SmartDashboard.putString("AutoSelected", _autoSelected);
		//			_autoSelected= _auto2;	


		if(_autoSelected == _autoL1) {
			_angleMod1 = -1;
			_angleMod2 = 1;
		}
		
		System.out.println("Angle Mods: 1: " + _angleMod1 + " 2: " + _angleMod2);

		if(this._autoSelected == this._autoR1) {
			_startSide = 'R';
			_notStartSide = 'L';
		}

		//these segments are from our drawing in one note.
		double segmentG = 66; // was 66 (base of triangle if we are C and we go to the left side of the switch
		double segmentI = 54; //was 54 (base of triangle if we are C and we goto the right side of the switch
		double segmentN = 75;//was 83 (height of both triangles for center auto)

		double segmentH = Math.sqrt((Math.pow(segmentI, 2)) + (Math.pow(segmentN, 2))); //(hypotenuse of triangle if we are C and we go to the right side of the switch)
		double segmentM = Math.sqrt((Math.pow(segmentG, 2)) + (Math.pow(segmentN, 2))); //was 106 (hypotenuse of triangle if we are C and we go to the left side of the switch)
		double segmentO = (105 - segmentN)/2;//distance we have to drive forward before and after we drive for Center//first number was 95
		double angleA = 90 - (Math.toDegrees(Math.atan(segmentN/segmentG))); //angle to rotate when we start if we go to the left side of the switch for C
		double angleC = 90 - (Math.toDegrees(Math.atan(segmentN/segmentI)));  //angle to rotate when we start if we go to the right side of the switch for C

		System.out.println(angleC);
		switch (_autoSelected) {
		case _autoL1: 

			this.generateSideStartSteps();

			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train
			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"high speed",0,0,0,0));
			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for shifter",0.1,0,0,0));
			//			if(_crossCourtSelected == _autoCrossCourtYes && _gameData.charAt(1) == 'R') {
			//				//this is cross court going from left position to scale on the right
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,0.65,0.65,segmentA));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0,angleMod2 * 90));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,0.5,0.5,segmentJ));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"second rotation",0,0,0,angleMod1 * 90));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Third Leg",0,0.4,0.4,segmentK));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SCALE,"At Scale",1,0,0,0));
			//			}else if(_gameData.charAt(1) == 'L'){
			//				//this is going from leftmost position to scale on the left
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,0.5,0.5,segmentB));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SCALE,"At Scale",5,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0.5,angleMod2 * 45));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,0.4,0.4,segmentC));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
			//			}else if(_gameData.charAt(0) == 'L'){
			//				// this goes from leftmost position to switch
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,0.7,0.7,segmentE));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0,angleMod2 * 90));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentD));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
			//			}


			//note we set coastmode in teleop init, but setting it here is a good practice
			//_steps.add( new AutonomousStep(AutonomousStep.stepTypes.COASTMODE,"Coast Mode",0,0,0,0)); //Set COAST mode for drive train

			break;


		case _autoCenter: 
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0));
			_steps.add(new AutonomousStep(AutonomousStep.stepTypes.LOW_SPEED, "Low Speed",0,0,0,0));
			if (_gameData.charAt(0) == 'L') {
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"First Leg",0,1.0,1.0,segmentO));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,.5,-angleA));//halfspeed
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"Second Leg",0,1.0,1.0,segmentM));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Second rotation",0,0,0,angleA));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"Third Leg",0,1.0,1.0,segmentO));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));

			}else {
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"First Leg",0,1.0,1.0,segmentO));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0,angleC));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"Second Leg",0,1.0,1.0,segmentH));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Second rotation",0,0,0,-angleC));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"Third Leg",0,1.0,1.0,segmentO));
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
			}

			break;

		case _autoR1: 

			this.generateSideStartSteps();

			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train
			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"high speed",0,0,0,0));
			//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for shifter",0.1,0,0,0));
			//			if(_crossCourtSelected == _autoCrossCourtYes && _gameData.charAt(1) == 'L') {//Cross court
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,.5,0.5,segmentA));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0,-90));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentJ));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"second rotation",0,0,0,90));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Third Leg",0,.4,0.4,segmentK));
			//			}else if(_gameData.charAt(1) == 'R'){//going to right scale
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,1.0,1.0,segmentB));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SCALE,"At Scale",0.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",2,0,0,-35));//rotation speed is .5
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT_FOR_SCALE,"Wait for scale",0,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentC));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
			//			}else if(_gameData.charAt(0) == 'R'){
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,1.0,1.0,segmentE));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",1,0,0,-90));//1 second max
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentD));
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
			//			}else {
			//				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,1.0,1.0,segmentE));
			//			}

			//note we set coastmode in teleop init, but setting it here is a good practice
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.COASTMODE,"Coast Mode",0,0,0,0)); //Set COAST mode for drive train


			break;



			//----------------------------------------------------------------------------------------------------------------------------------

		case _autoRotator:
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"Low speed transmission",0,0,0,0)); //make sure we are in low speed
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Drive Forward1",0,1,1,30));
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Turn 60",0,0,0,90));
//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DEADRECON,"Drive Forward2",0,.5,.5,30));
//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Turn 60",0,0,0,-90));
//			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DEADRECON,"Drive Forward3",0,.5,.5,12));

			break;

		case _autoTestSpeed:
			caseName="Speed Test";
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train

			if(highSpeedTest)
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"High Speed",0,0,0,0));

			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE,"Drive",calibrationRunTime,speedTest,speedTest,0));
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.STOP,"Stop",0,0,0,0));

			if(highSpeedTest)
				_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for robot to stop",5,0,0,0));

			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.COASTMODE,"Coast Mode",0,0,0,0)); //Set COAST mode for drive train
			break;

		case _autoTestDistance:
			caseName="Distance Test";
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"Drive",0,speedTest,speedTest,60));
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.STOP,"Stop",0,0,0,0));
			break;

			case _driveForward:
				caseName="DriveForward";
				this.addDriveForward();
				break;


		default: 
			caseName="Do Nothing";
			_steps.add( new AutonomousStep(AutonomousStep.stepTypes.STOP,"Stop",0,0,0,0));
			break;
		}


		System.out.println("setAuto");
	}

	//	private void addPortcullisSteps(){
	//		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DEADRECON,"Forward to Defense", 0, 0.7, 0.7, (FIRST_LEG_DISTANCE - (Enums.ROBOT_LENGTH_EXTENDED - OVERHANG ) +35)/12.0));
	//		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.LIFT,"Move shifter home", 0.5, 0, 0, 0));			
	//		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DEADRECON,"Navigate Defense", 0, 0.6, 0.6, (DEFENSE_DEPTH - OVERHANG + Enums.ROBOT_LENGTH_EXTENDED)/12.0));
	//	}


	private void addSideRunSteps(boolean isBlueAlliance,boolean isBoilerSide){
		double rotationAngle = 60;
		if (!isBoilerSide){	
			rotationAngle = (isBlueAlliance ? 60 : -60);
		} else {			
			rotationAngle = (isBlueAlliance ? -60 : 60);
		}

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_DISTANCE,"backup some more",0,-0.9,-0.9,20)); //back up some more
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Turn to feeder",0,0,0,rotationAngle));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE,"start moving",0.5,0.5,0.5,0)); //move some
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"HI speed transmission",0,0,0,0)); //swich tranny
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE,"Drive Down Field",1.0,0.75,0.75,0)); //drive down the field


	}

	private void generateSideStartSteps() {

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.BRAKEMODE,"Brake Mode",0,0,0,0)); //Set brake mode for drive train
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.HIGH_SPEED,"high speed",0,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for shifter",0.1,0,0,0));
		//_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Delay for alliance",9.0,0,0,0));//added for 4253
		if(_scoringPreference == this._scoringPreferenceScale) {
			if(_gameData.charAt(1) == _notStartSide && _crossCourtSelected == _autoCrossCourtYes) {
				this.addCrossCoutSteps();
				this._scoringSide = this._notStartSide;
			}else if(_gameData.charAt(1) == _startSide) {
				System.out.println("Going To Scale");
				this.addScaleSteps();
				this._scoringSide = this._startSide;
				if(this._gameData.charAt(0) == _startSide)
					this.add2Cube();
			}else if(_gameData.charAt(0) == _startSide) {
				this._scoringSide = this._startSide;
				this.addSwitchSteps();
			}else {
				this.addDriveForward();
			}
		}else if(_scoringPreference == this._scoringPreferenceSwitch) {
			if(_gameData.charAt(0) == _startSide) {
				this._scoringSide = this._startSide;
				this.addSwitchSteps();
			}else if(_gameData.charAt(1) == _notStartSide && _crossCourtSelected == _autoCrossCourtYes){
				this._scoringSide = this._notStartSide;
				this.addCrossCoutSteps();
			}else if(_gameData.charAt(1) == _startSide) {
				this._scoringSide = this._startSide;
				this.addScaleSteps();
			}else {
				this.addDriveForward();
			}
		}

	}

	private void addScaleSteps(){
		double segmentB = 260; //was 285
		double segmentC = 8; //was 21
		double segmentD = 60;
		double segmentE = 10;
		double segmentF = 13;

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,1.0,1.0,segmentB));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SCALE,"At Scale",0.1,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",2,0,0,_angleMod2 * 45));//rotation speed is .5
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT_FOR_SCALE,"Wait for scale",0,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentC));
//		if(!this.checkGameData(1))
//			return;
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.AQUISITION_OUT,"Acquisition arms out",0,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"cube out",Enums.RELEASE_TIMER,0,0,0));
		//_steps.add(new AutonomousStep(AutonomousStep.stepTypes.AQUISITION_IN,"arms in",0,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"back up",0,-.4,-0.4,segmentC));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.GOTO_BOTTOM,"lower elevator",0,0,0,0));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.FOUR_BAR_DOWN,"Lower 4 bar",0.1,0,0,0));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"Second Rotation",0,0,0,_angleMod2 * 85));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"fifth leg",0,0.8,0.8,segmentD));
//		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"third rotation",2,0,0,_angleMod2 * 45));
//		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"sixth leg",0,.5,.5,segmentE));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.CUBE_IN,"cube in",0.1,0,0,0));
//		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"seventh leg",0,0.5,0.5,segmentF));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.AQUISITION_IN,"arms in",0,0,0,0));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"grab cube",0,-.4,-.4,segmentE));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.FOUR_BAR_UP,"4 bar up",0.1,0,0,0));
	}

	private void addSwitchSteps(){
		double segmentD = 6; //was 34
		double segmentE = 134; //was 129//was 140

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,1.0,1.0,segmentE));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Let robot settle",0.2,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",1,0,0,_angleMod2 * 90));//1 second max
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,.4,0.4,segmentD));
//		if(!this.checkGameData(0))
//			return;
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"reverse",0,-.5,-.5,segmentD));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"second rotation",0,0,0,_angleMod1 * 90));
	}

	private void addCrossCoutSteps(){
		double segmentA = 220; //was 235
		double segmentJ = 195; //was 207
		double segmentK = 24; //was 85
		double segmentM = 12;

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SWITCH,"At Switch",.1,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0.0,1.0,1.0,segmentA));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.WAIT,"let robot settle",0.2,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"First rotation",0,0,0,_angleMod2 * 90));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Second Leg",0,0.8,0.8,segmentJ));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.WAIT,"let robot settle",0.2,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.ROTATE,"second rotation",0,0,0,_angleMod1 * 90));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for rotate",0.1,0,0,0));
		//_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Third Leg",0,0.4,0.4,segmentK));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.GOTO_SCALE,"Go To Scale",5,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"Fourth Leg",0,0.4,0.4,segmentM));
//		if(!this.checkGameData(1))
//			return;
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.AQUISITION_OUT,"arms out",0,0,0,0));
		_steps.add(new AutonomousStep(AutonomousStep.stepTypes.WAIT,"Wait for arms",0.2,0,0,0));
		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.CUBEOUT,"Cube out",Enums.RELEASE_TIMER,0,0,0));
	}

	private void addDriveForward(){
		double segmentL = 100;

		_steps.add( new AutonomousStep(AutonomousStep.stepTypes.DRIVE_PROFILE,"First Leg",0,0.7,0.7,segmentL));
	}
	
	private void add2Cube() {
		
	}
	
	private boolean checkGameData( int index) {
		_gameData = DriverStation.getInstance().getGameSpecificMessage();
		if(_gameData.charAt(index) == this._scoringSide) {
			return true;
		}else
			return false;
	}


	public boolean isExecuting(){
		return _isExecuting;
	}
	public void cancelExecution(){
		_cancelExecution = true;
		_controls.setCoastMode();
	}

	public void execute ()
	{
		double encPos = 0; //moved from drivedistance because was initialized twice
		//		System.out.println("auto execute");
		if (_steps == null ){
			_isExecuting = false;
			return;

		}

		if (_steps.size() == 0){
			System.out.println("trying to execute no steps in Autonomous 360");
			_isExecuting = false;	
			return;
		}

		double cTime=0;


		if (_autoTimer.get() > _totalAutoTime || _currentStep >= _steps.size() || _cancelExecution){
			System.out.println("Autonomous Completed: "+ _autoTimer.get());
			_controls.getDrive().set(0, 0, 0, 0);
			_isExecuting = false;	
			disable();
		}
		else
		{
			_currentAutoStep = _steps.get(_currentStep);

			_isExecuting = true;
			//switch (_steps.get(_currentStep))
			switch (_currentAutoStep.stepType)
			{
			case DRIVE:
				drive(_currentAutoStep.stepTime,_currentAutoStep.leftSpeed,_currentAutoStep.rightSpeed);
				break;

			case DRIVE_DISTANCE:
				if(Enums.IS_FINAL_ROBOT) {
					if (_controls.getIsSlowMode())
						encPos = Math.abs((_currentAutoStep.distance - 1.9029236202) /0.0051668741);//was 0.0051668741
					else
						encPos = Math.abs((_currentAutoStep.distance + 3.1365268239) /0.00582985076625);//was 0.0052878465
				} else {
					if (_controls.getIsSlowMode())
						encPos = Math.abs((_currentAutoStep.distance + 4.9437275823) /0.00533638);
					else
						encPos = Math.abs((_currentAutoStep.distance + 1.1531168803) /0.0061007236);
				}

				//				if (_controls.getIsSlowMode())
				//					encPos = Math.abs((_currentAutoStep.distance + 1.1531168803) /0.0061007236);
				//				else
				//					encPos = Math.abs((_currentAutoStep.distance + 4.9437275823) /0.00533638);
				driveStraight(0,_currentAutoStep.leftSpeed,_currentAutoStep.rightSpeed,encPos);

				break;

			case DRIVE_PROFILE:
				if (Enums.IS_FINAL_ROBOT)	{				
					if (_controls.getIsSlowMode())
						encPos = Math.abs((_currentAutoStep.distance - 1.9029236202) /0.0051668741);//was 0.0051668741
					else {
						encPos = Math.abs((_currentAutoStep.distance + 3.1365268239) /0.00582985076625);//was 0.0052878465

					}

				}else {
					//System.out.println("Slow mode in Auto: " +_controls.getIsSlowMode());
					if (_controls.getIsSlowMode())
						encPos = Math.abs((_currentAutoStep.distance + 4.9437275823) /0.00533638);
					else
						encPos = Math.abs((_currentAutoStep.distance + 1.1531168803) /0.0061007236);


					//					if (_controls.getIsSlowMode())
					//						encPos = Math.abs((_currentAutoStep.distance + 1.1531168803) /0.0061007236);
					//					else
					//						encPos = Math.abs((_currentAutoStep.distance + 4.9437275823) /0.00533638);

				}

				driveStraightProfile(0,_currentAutoStep.leftSpeed,_currentAutoStep.rightSpeed,encPos);

				break;

			case DRIVE_DEADRECON:

				driveStraight(cTime,_currentAutoStep.leftSpeed,_currentAutoStep.rightSpeed,0);
				break;

			case CALCANGLE:
				if (!_angleHasBeenCalculated){

					if (DriverStation.getInstance().getAlliance() == DriverStation.Alliance.Blue){
						//this is where we calculate the rotation angle
						//initial heading + 225 - current heading should be pretty close
						_calculatedAngle = 180 - 30;//_initCompassHeading + 225 - _navx.getCompassHeading();
					}	else{
						_calculatedAngle = 180 + 30; //_initCompassHeading + 135.0 - _navx.getCompassHeading();

					}

					_angleHasBeenCalculated = true;
				}
				SmartDashboard.putNumber("Init Compass Heading", _initCompassHeading);
				SmartDashboard.putNumber("CalcAngle", _calculatedAngle);
				//now just rotate the calculated distance
				rotate(_calculatedAngle);
				break;

			case GET_ANGLE_CORRECTION:
				//getVisionCorrection(_currentAutoStep.stepTime);
				break;


			case ROTATE:
				rotate(_currentAutoStep.distance, _currentAutoStep.stepTime);

				break;


			case BRAKEMODE:
				_controls.setBrakeMode();
				_controls.getDrive().zeroEncoder();
				startNextStep();
				break;

			case COASTMODE:
				_controls.setCoastMode();
				startNextStep();
				break;

			case LOW_SPEED:
				_controls.setLowSpeed();
				startNextStep();
				break;

			case HIGH_SPEED:
				_controls.setHighSpeed();
				startNextStep();
				break;


			case WAIT:
				wait(_currentAutoStep.stepTime);
				break;

			case WAIT_FOR_SCALE:
				this.waitForScale();
				break;

			case STOP:
				stop();
				break;

			case GOTO_SWITCH:
				this.goToSwitch(_currentAutoStep.stepTime);
				break;

			case GOTO_SCALE:
				this.goToScale(_currentAutoStep.stepTime,_currentAutoStep.rightSpeed);
				break;

			case GOTO_BOTTOM:
				this.goToBottom();
				break;

			case CUBEOUT:
				this.cubeOut(_currentAutoStep.stepTime);
				break;

			case CUBE_IN:
				this.cubeIn(_currentAutoStep.stepTime);
				break;

			case FOUR_BAR_UP:
				this.fourBarUp(this._currentAutoStep.stepTime);
				break;

			case FOUR_BAR_DOWN:
				this.fourBarDown(this._currentAutoStep.stepTime);
				break;

			case AQUISITION_IN:
				this._controls.getAcquisition().armsIn();
				this.startNextStep();
				break;

			case AQUISITION_OUT:
				this._controls.getAcquisition().armsOut();
				startNextStep();
				break;


			}
		}

		this.updateStatus();
		_controls.updateStatus();
	}


	private void rotate(double distance) {
		rotate(distance,0);
	}

	private void rotate( double distance, double time){
		double deltaYaw =0.0;
		double speed;
		
		double lowSpeed;
		double maxSpeed;


		if(this._controls.getIsSlowMode()) {
			lowSpeed = 0.2;
			maxSpeed = 0.4;
		}else {
			lowSpeed = 0.28;
			maxSpeed = 0.5;
		}

		double tolerance=0.50; // be within this angle to stop


		if (distance == 0){
			startNextStep();
			return;
		}

		if (_stepTimer.get() >= time && time> 0)
		{
			_controls.getDrive().set(0, 0, 0, 0);
			startNextStep();
		}
		else {

			// deltaYaw = current angle - setpoint ; the sign here will be corrective... to go 90 then the direction = -1, to go -90 the direction = 1
			deltaYaw = _controls.getNavx().getYaw() - distance ;


			if(Math.abs(deltaYaw) > tolerance){
//				SmartDashboard.putNumber("Auto Yaw", _controls.getNavx().getYaw());

				// deltaYaw / distance = 100% of change use the nominal rate

				//= 0.5 + -90/90 * (0.8 - 0.5);


				speed =  lowSpeed + Math.abs(deltaYaw / distance) * ( maxSpeed - lowSpeed );
				speed = (deltaYaw > 0) ? speed : -speed;


				//however, we want to set a lower floor on the speed because the motor stalls
				//				speed = (Math.abs(speed) < lowSpeed) ? -direction * lowSpeed : speed ;

				System.out.println("Offset: " + deltaYaw + " rotate speed: "+speed);
//				SmartDashboard.putNumber("Rotate Offset", deltaYaw);

				_controls.getDrive().set(speed, speed, -speed, -speed);

			}
			else {
				System.out.println("Rotation condition met: " + deltaYaw + "  tolerance " +tolerance);
				_controls.getDrive().set(0, 0, 0, 0);
				startNextStep();
			}

		}
		//System.out.println("Rotating: "+ distance + " speed "+speed);

	}


	//	private void rotate( double distance, double speed,double time){
	//		double deltaYaw;
	//		//	double  speed = 0.3;
	//		if (speed == 0) {
	//			speed = .3;
	//		}
	//		double lowSpeed = 0.3;
	//		double tolerance=0.50;
	//		int direction=1;
	//
	//
	//		if (distance == 0){
	//			startNextStep();
	//			return;
	//		}
	//		
	//		if (_stepTimer.get() >= time && time> 0)
	//			startNextStep();
	//		else {
	//
	//
	//
	//
	//			//deltaYaw = _initialYaw + _controls.getNavx().getYaw();
	//			//SmartDashboard.putNumber("deltaYaw", deltaYaw);
	//			// 
	//			direction = (distance > 0 ? -1 : 1);
	//			speed = direction * speed;	
	//			_controls.getDrive().set(speed, speed, -speed, -speed);
	//
	//
	//			deltaYaw = _controls.getNavx().getYaw() - distance ;
	//					
	//			if(Math.abs(_controls.getNavx().getYaw()) > Math.abs(distance)){
	//				SmartDashboard.putNumber("Auto Yaw", _controls.getNavx().getYaw());
	//				
	//				speed=-speed/1.5;
	//				
	//				//speed = (Math.abs(speed) < lowSpeed) ? -direction * lowSpeed : speed ;
	//				
	//				System.out.println("Offset: " + (Math.abs(_controls.getNavx().getYaw()) - Math.abs(distance)) * direction + " rotate speed: "+speed);
	//				SmartDashboard.putNumber("Rotate Offset", (Math.abs(_controls.getNavx().getYaw()) - Math.abs(distance))*direction);
	//				_controls.getDrive().set(speed, speed, -speed, -speed);
	//				if(Math.abs(_controls.getNavx().getYaw())-Math.abs(distance)<=tolerance){
	//					_controls.getDrive().set(0, 0, 0, 0);
	//					startNextStep();
	//				}
	//			}
	//			//System.out.println("Rotating: "+ distance + " speed "+speed);
	//
	//		}
	//	}



	public void updateStatus(){

		if (_steps != null && _currentAutoStep != null){
//			SmartDashboard.putNumber("Step Count", _steps.size());
//			SmartDashboard.putString("Current Command", this._currentStep + " " + _currentAutoStep.name  + "\n " + _currentAutoStep.stepTime);			
		}

		if(_gameDataFailed){
			_gameData = DriverStation.getInstance().getGameSpecificMessage();
			if(this._gameData.length() > 2){
				this._gameDataFailed = false;
				this.setAuto();
			}
			if(this._dataTimer.get() > 10){
				System.out.println("No Game Data After 10 Seconds, driving forward");
				this._autoSelected = this._driveForward;
				setAuto();
			}

		}



	}
	public void drive (double time, double left, double right)
	{


		if (_stepTimer.get() > time)
		{
			_controls.getDrive().set(0, 0, 0, 0);
			//SmartDashboard.putNumber("Encoder Value", _controls.getDrive().getAbsAvgEncoderValue());
			startNextStep();
		}
		else
		{
			_controls.getDrive().set(-left, -left, -right, -right);
		}
	}

	public void driveStraight (double time, double left, double right,double targetEncPosition)
	{
		float deltaYaw;

		//deltaYaw = _initialYaw - _controls.getNavx().getYaw();
		deltaYaw = _navx.getYaw();
		double offsetLimit = 0.05;
		double offset=0;

		//		SmartDashboard.putNumber("currentYaw", _initialYaw);
		//		SmartDashboard.putNumber("deltaYaw", deltaYaw);
		if (left == right){
			offset = Math.abs(deltaYaw);
			if(offset > offsetLimit){
				offset = offsetLimit;
			}
			if(deltaYaw < 0){
				left = left * (1+offset);
				right = right * (1-offset);
			}else{
				left = left * (1-offset);
				right = right * (1+offset);
			}
		}

		//		SmartDashboard.putNumber("Auto Enc", _controls.getDrive().getAbsAvgEncoderValue());
		if (targetEncPosition > 0){
			if (_controls.getDrive().getAbsAvgEncoderValue() >= targetEncPosition)
			{
				_controls.getDrive().set(0, 0, 0, 0);
				System.out.println("Target encoder position" + targetEncPosition);
				System.out.println("Current encoder position" + _controls.getDrive().getAbsAvgEncoderValue());
				startNextStep();
			}
			else
			{
				_controls.getDrive().set(-left, -left, -right, -right);
			}

		} else {
			if (_stepTimer.get() >= time)
			{
				_controls.getDrive().set(0, 0, 0, 0);
				System.out.println("Target encoder position" + targetEncPosition);
				System.out.println("Current encoder position" + _controls.getDrive().getAbsAvgEncoderValue());
				startNextStep();
			}
			else
			{
				_controls.getDrive().set(-left, -left, -right, -right);
			}
		}
	}

	public void driveStraightProfile (double time, double left, double right,double targetEncPosition)
	{
		double remainingDistance = 0;
		//double remainingPercent = 0;
		remainingDistance = targetEncPosition - _controls.getDrive().getAbsAvgEncoderValue();
		//remainingPercent = remainingDistance/targetEncPosition;
		double leftsign = (left >= 0) ? 1.0 : -1.0;
		double rightsign = (right >= 0) ? 1.0 : -1.0;


		if (remainingDistance <= 6000) {
			left =  (Math.abs(left) > 0.30) ?  0.30 * leftsign : left ;  //only brake if setpoint is greater than setpoint
			right = (Math.abs(right) > 0.30) ? 0.30 * rightsign : right;
		}
		//	else if (remainingDistance <= 4000){
		//		left = 0.35 * leftsign;
		//		right = 0.35 * rightsign;
		//	}else if (remainingDistance <= 5000){
		//		left = 0.40 * leftsign;
		//		right = 0.40 * rightsign;
		//	}else if (remainingDistance <= 6000){
		//		left = 0.45 * leftsign;
		//		right = 0.45 * rightsign;
		//	}else if (remainingDistance <= 7000){
		//		left = 0.50 * leftsign;
		//		right = 0.50 * rightsign;
		//	}	

		float deltaYaw;

		//deltaYaw = _initialYaw - _controls.getNavx().getYaw();
		deltaYaw = _navx.getYaw();
		double offsetLimit = 0.05;
		double offset=0;

		//		SmartDashboard.putNumber("currentYaw", _initialYaw);
		//		SmartDashboard.putNumber("deltaYaw", deltaYaw);
		if (left == right){
			offset = Math.abs(deltaYaw);
			if(offset > offsetLimit){
				offset = offsetLimit;
			}
			if(deltaYaw < 0){
				left = left * (1+offset);
				right = right * (1-offset);
			}else{
				left = left * (1-offset);
				right = right * (1+offset);
			}
		}

		//		SmartDashboard.putNumber("Auto Enc", _controls.getDrive().getAbsAvgEncoderValue());
		if (targetEncPosition > 0){
			if (_controls.getDrive().getAbsAvgEncoderValue() >= targetEncPosition)
			{
				_controls.getDrive().set(0, 0, 0, 0);
				System.out.println("Target encoder position" + targetEncPosition);
				System.out.println("Current encoder position" + _controls.getDrive().getAbsAvgEncoderValue());
				startNextStep();
			}
			else
			{
				_controls.getDrive().set(-left, -left, -right, -right);
			}

		} else {
			if (_stepTimer.get() >= time)
			{
				_controls.getDrive().set(0, 0, 0, 0);
				System.out.println("Target encoder position" + targetEncPosition);
				System.out.println("Current encoder position" + _controls.getDrive().getAbsAvgEncoderValue());
				startNextStep();
			}
			else
			{
				_controls.getDrive().set(-left, -left, -right, -right);
			}
		}
	}
	public void drive (double time, double leftX, double leftY, double rightX, double rightY)
	{
		if (_stepTimer.get() > time)
		{
			_controls.getDrive().set(0, 0, 0, 0);
			startNextStep();
		}
		else
		{
			_controls.getDrive().set(leftX, leftY, rightX, rightY);
		}
	}

	public void grab (int grabSetpoint)
	{
		//			_controls.getElevator().setGrabMotor(grabSetpoint);
		startNextStep();
	}




	public void goToSwitch (double time) {
		if (_stepTimer.get() > time)
			startNextStep();
		else {
			_controls.getElevator().goToSwitch();
		}
	}

	public void goToScale (double time,double encoderValue) {
		if (_stepTimer.get() > time || _controls.getElevator().isAtScale() ||_controls.getElevator().scaleEncoder() >= encoderValue && encoderValue !=0)
			startNextStep();
		else {
			_controls.getElevator().goToScale();
		}
	}

	public void cubeOut (double time) {
		if (_stepTimer.get() > time) {
			startNextStep();
		}else {
			_controls.getAcquisition().cubeOut();
		}
	}

	public void cubeIn (double time) {
		if (_stepTimer.get() > time) {
			startNextStep();
		}else {
			this._controls.getAcquisition().cubeIn();
		}
	}

	public void fourBarDown(double time){
		if(_stepTimer.get() > time)
			startNextStep();
		else
			this._controls.getAcquisition().autoLowerLinkage();
	}

	public void fourBarUp(double time){
		if(this._stepTimer.get() > time)
			this.startNextStep();
		else
			this._controls.getAcquisition().autoRaiseLinkage();
	}

	public void goToBottom () {
		if(_controls.getElevator().isAtBottom()) {
			startNextStep();
		}else {
			_controls.getElevator().goToBottom();
		}
	}


	public void wait (double time)
	{
		if (_stepTimer.get() > time)
			startNextStep();
	}

	public void waitForScale() {
		if((this._controls.getElevator().getTargetLimit() == null || this._controls.getElevator().isAtTarget() || Math.abs(this._controls.getElevator().scaleEncoder() - this._controls.getElevator().getTargetEncoder()) <= Enums.ELEVATOR_ENCODER_SAFETY))
			this.startNextStep();
	}


	public void stop ()
	{
		_controls.getDrive().set(0, 0, 0, 0);
		startNextStep();
	}

	public void startNextStep ()
	{
		System.out.println("Step "+_currentStep + " "+ _stepTimer.get() + " s  --"+   _currentAutoStep.name  + "-- is completed. EncPos= "+_controls.getDrive().getAbsAvgEncoderValue());
		_navx.zeroYaw();
		//		SmartDashboard.putNumber("Starting Yaw", _controls.getNavx().getYaw() );
		_currentStep++;
		_stepTimer.reset();
		_angleHasBeenCalculated=false;
		_controls.getDrive().zeroEncoder();		
	}	

	private void ReadAutoFile(){
		BufferedReader br = null;

		try {

			String sCurrentLine;
			String temp[];	
			String sComment = "\\\\";
			String sSteps = "";
			AutonomousStep newStep;


			//			br = new BufferedReader(new FileReader("C:\\autonomous\\testing.txt"));
			br = new BufferedReader(new FileReader("/home/lvuser/autonomous.txt"));

			while ((sCurrentLine = br.readLine()) != null) {				
				if (sCurrentLine.startsWith(sComment)){
					System.out.println("Comment: ingoring -> "+ sCurrentLine);
				} else{
					System.out.println(sCurrentLine);
					sSteps = sCurrentLine + "\n" + sSteps; 
					temp = sCurrentLine.split(",");
					newStep =  new AutonomousStep();
					newStep.name = temp[1];
					newStep.stepType = AutonomousStep.stepTypes.valueOf( temp[0]);
					newStep.stepTime = Double.parseDouble(temp[2]);
					newStep.leftSpeed = Double.parseDouble(temp[3]);
					newStep.rightSpeed = Double.parseDouble(temp[4]);
					newStep.distance = Double.parseDouble(temp[5]);
					_steps.add(newStep);

					System.out.println("Autosteps now has "+_steps.size());
				}
			}
			SmartDashboard.putString("File Steps", sSteps);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}			

}
