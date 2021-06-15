/*
	Name: 		Dylan Kapustka (dlk190000)
	Instructor: Professor Ozbirn
	Class:      CS 4348.001 - S21
	Date: 		4-10-2021
*/

import java.util.concurrent.Semaphore;

public class Project2{

    //Semaphores Declared
    private static Semaphore patient_enters; //Patient enters clinic
	private static Semaphore receptionist_ready; //Receptionist ready
	private static Semaphore receptionist_registers; //Receptionist registers patient
	private static Semaphore declared_office; //Office assigned for registered patient
	private static Semaphore receptionist_leaves; //Release receptionist
	private static Semaphore patient_waiting[]; //Patients in waiting room ([] for each doc office)
	private static Semaphore nurse_notified[]; //Nurse notified
	private static Semaphore nurse_available[]; //Nurse available
	private static Semaphore nurse_directs_to_office[]; //Nurse directs patient to office
	private static Semaphore patient_waits_in_office[]; //Patient waits in designated office
	private static Semaphore doctor_notified[]; //Doctor notified
	private static Semaphore doctor_available[]; //Doctor available
	private static Semaphore doctor_listens_to_symptoms[]; //Doctor listens to patient symptoms
	private static Semaphore doctor_advises[]; //doctor advises patient
	private static Semaphore patient_leaves[]; //Patient leaves office

	//Resources
	private static int registerPatientID;
	private static int officeNumber;
	private static int doctor_num;
    private static int nurse_num;
	private static int patient_num;
	private static int waiting_for_office[] = new int[3];
	private static int in_office[] = new int[3];


    public static void main(String args[]) {

        // Check for two arguments expected
        if (args.length != 2) {
            System.out.println("Error: Incorrect Arguments. Please enter the number of doctors and number of patients.");
            System.exit(0);
        }
        if(Integer.parseInt(args[0]) > 3){
            System.out.println("Error: Too many doctors!");
            System.exit(0);
        }
        if(Integer.parseInt(args[1]) > 30){
            System.out.println("Error: Too many patients!");
            System.exit(0);
        }

        doctor_num = Integer.parseInt(args[0]);
        nurse_num = Integer.parseInt(args[0]); //Nurse number should match doctor
        patient_num = Integer.parseInt(args[1]);

        System.out.printf("Run with %d doctors, %d nurses, %d patients%n%n", doctor_num, nurse_num, patient_num);

        //Initialize Semaphores
		patient_enters = new Semaphore(0, true);
		receptionist_ready = new Semaphore(0, true);
		receptionist_registers = new Semaphore(0,true);
		declared_office = new Semaphore(0,true);
		receptionist_leaves = new Semaphore(0,true);
		patient_waiting = new Semaphore[doctor_num];
		nurse_notified = new Semaphore[doctor_num];
		nurse_available = new Semaphore[doctor_num];
		nurse_directs_to_office = new Semaphore[doctor_num];
		patient_waits_in_office = new Semaphore[doctor_num];
		doctor_notified = new Semaphore[doctor_num];
		doctor_available = new Semaphore[doctor_num];
		doctor_listens_to_symptoms = new Semaphore[doctor_num];
		doctor_advises = new Semaphore[doctor_num];
		patient_leaves = new Semaphore[doctor_num];
		for(int i = 0; i < doctor_num; i++){
			patient_waiting[i] = new Semaphore(0, true);
			nurse_notified[i] = new Semaphore(0, true);
			nurse_available[i] = new Semaphore(0, true);
			nurse_directs_to_office[i] = new Semaphore(0, true);
			patient_waits_in_office[i] = new Semaphore(0, true);
			doctor_notified[i] = new Semaphore(0, true);
			doctor_available[i] = new Semaphore(0, true);
			doctor_listens_to_symptoms[i] = new Semaphore(0, true);
			doctor_advises[i] = new Semaphore(0, true);
			patient_leaves[i] = new Semaphore(1, true);
		}


		//Begin Threads
		Thread Receptionist = new Thread(new Receptionist());
		Receptionist.start();

		Thread doctor[] = new Thread[doctor_num];
		Thread nurse[] = new Thread[doctor_num];
		for(int i = 0; i < doctor_num; i++){
			doctor[i] = new Thread(new Doctor(i));
			nurse[i] = new Thread(new Nurse(i));

			doctor[i].start();
			nurse[i].start();
		}

		Thread patient[] = new Thread[patient_num];
		for(int i = 0; i < patient_num; i++){
			patient[i] = new Thread(new Patient(i));
			patient[i].start();
		}


		for(int i = 0; i < patient_num; i++){
			try{
				patient[i].join();
			} catch (InterruptedException e) {}
		}



		System.exit(0);


	}

	//Receptionist class
	static class Receptionist implements Runnable{

		public void run(){
			try{
				while(true){
					//Receptionist waits for patient to enter
					receptionist_ready.release();
					patient_enters.acquire();

					//Receptionist gets patient ID and assigns a random doctor office
					receptionist_registers.acquire();
					System.out.printf("Receptionist registers patient %d.%n", registerPatientID);
					 
					officeNumber = new java.util.Random().nextInt(doctor_num);
					declared_office.release();

					//Notifies the nurse
					nurse_notified[officeNumber].release();

					//Receptionist waits on patient to leave
					receptionist_leaves.acquire();
				}
			} catch(InterruptedException e){System.out.println("InterruptedException in Receptionist");}
		}


	}

//Doctor class
	static class Doctor implements Runnable{
		private int doctorID;

		//ID of doctor -- must match nurse
		public Doctor(int id){ this.doctorID = id; }

		public void run(){
			try{
				while(true){
                    //Doctor is available
					doctor_available[doctorID].release();

					//Waot to be notified
					doctor_notified[doctorID].acquire();
					patient_waits_in_office[doctorID].acquire();

					//Doctor listens to symptoms
					doctor_listens_to_symptoms[doctorID].acquire();
					System.out.printf("Doctor %d listens to symptoms from patient %d. %n", doctorID, in_office[doctorID]);

					//	Doctor gives advices from symptoms
					doctor_advises[doctorID].release();

				}

			} catch(InterruptedException e){System.out.println("InterruptedException in Doctor " + doctorID);}

		}
	}

//Patient class
	static class Patient implements Runnable{
		private int patientID;
		private int assignedOffice;

//Unique ID to patient
		public Patient(int id){ this.patientID = id; }

		public void run(){
			try{

				
				receptionist_ready.acquire();
				//Patient enters and waits for receptionist
				patient_enters.release();
				System.out.printf("Patient %d enters waiting room, waits for Receptionist. %n", patientID);

				//Patient is registered
				registerPatientID = patientID;
				receptionist_registers.release();

				//Patient waits for assigned office
				declared_office.acquire();
				assignedOffice = officeNumber;
				System.out.printf("Patient %d leaves receptionist and sits in waiting room. %n", patientID);
				 
                //Release receptionist
				receptionist_leaves.release();

				//Patient waits on nurse
				patient_waiting[assignedOffice].release();
				nurse_available[assignedOffice].acquire();
				waiting_for_office[assignedOffice] = patientID;
				nurse_directs_to_office[assignedOffice].acquire();

				//Patient waits in designated office
				System.out.printf("Patient %d enters doctor %d's office. %n", patientID, assignedOffice);
				 
				in_office[assignedOffice] = patientID;
				patient_waits_in_office[assignedOffice].release();
				doctor_available[assignedOffice].acquire();

				//Patient shares symptoms
				doctor_listens_to_symptoms[assignedOffice].release();
				doctor_advises[assignedOffice].acquire();
				System.out.printf("Patient %d receives advice from doctor %d. %n", patientID, assignedOffice);
				 

				//Patient leaves
				System.out.printf("Patient %d leaves. %n", patientID);
				patient_leaves[assignedOffice].release();

			} catch(InterruptedException e){System.out.println("InterruptedException in Patient " + patientID);}

		}
	}


//Nurse class
	static class Nurse implements Runnable{
		private int nurseID;

//ID matches doctor ID
		public Nurse(int id){ this.nurseID = id; }

		public void run(){
			try{
				while(true){
                    //Nurse is available
					nurse_available[nurseID].release();

					//Wait for receptionist to notify
					patient_waiting[nurseID].acquire();
					nurse_notified[nurseID].acquire();

					//Nurse waits on empty office
					patient_leaves[nurseID].acquire();

					//Nurse directs patient to office
					System.out.printf("Nurse %d takes patient %d to doctor's office. %n", nurseID, waiting_for_office[nurseID]);
					nurse_directs_to_office[nurseID].release();

                    //Nurse notifies doctor
					doctor_notified[nurseID].release();
				}
			} catch(InterruptedException e){System.out.println("InterruptedException in Nurse " + nurseID);}

		}
	}
}