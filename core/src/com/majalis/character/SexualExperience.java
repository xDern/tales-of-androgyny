package com.majalis.character;

public class SexualExperience {

	private final int analSex;
	private final int creampies;
	private final int analEjaculation;
	private final int oralSex;
	private final int oralCreampies;
	private final int fellatioEjaculation;
	private final int bellyful;
	private final int handy;
	private final boolean horse;
	private final boolean ogre;
	private final boolean prostitution;
	private final boolean beast;
	private final boolean bird;
	private final boolean knot;
	
	public static class SexualExperienceBuilder {
		
		private int analSex;
		private int creampies;
		private int analEjaculation;
		private int oralSex;
		private int oralCreampies;
		private int fellatioEjaculation;
		private boolean horse;
		private boolean ogre;
		private boolean bird;
		private boolean knot;
		
		public SexualExperienceBuilder() {
			this (0);
		}
		
		public SexualExperienceBuilder(int anal) {
			this.analSex = anal;
			horse = false;
		}

		public SexualExperienceBuilder setAnalSex(int anal, int analCreampies, int analEjaculation) {
			this.analSex = anal;
			this.creampies = analCreampies;
			this.analEjaculation = analEjaculation;
			return this;
		}
		
		public SexualExperienceBuilder setAnalEjaculations(int num) {
			analEjaculation = num;
			return this;
		}
		
		public SexualExperienceBuilder setOralSex(int num) {
			oralSex = num;
			return this;
		}
		
		public SexualExperienceBuilder setOralCreampie(int num) {
			oralCreampies = num;
			return this;
		}
		
		public SexualExperienceBuilder setHorse() {
			horse = true;
			return this;
		}
		
		public SexualExperienceBuilder setOgre() {
			ogre = true;
			return this;
		}
		
		public SexualExperienceBuilder setBird() {
			bird = true;
			return this;
		}
		
		public SexualExperience build() {
			return new SexualExperience(analSex, creampies, analEjaculation, oralSex, oralCreampies, fellatioEjaculation, 0, 0, horse, ogre, false, false, bird, knot);
		}		
	}
	
	private SexualExperience() { this(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, false, false, false); }
	
	private SexualExperience(int analSex, int creampies, int analEjaculation, int oralSex, int oralCreampies, int fellatioEjaculation, int bellyful, int handy, boolean horse, boolean ogre, boolean prostitution, boolean beast, boolean bird, boolean knot) {
		this.analSex = analSex;
		this.creampies = creampies;
		this.analEjaculation = analEjaculation;
		this.oralSex = oralSex;
		this.oralCreampies = oralCreampies;
		this.fellatioEjaculation = fellatioEjaculation;
		this.bellyful = bellyful;
		this.handy = handy;
		this.horse = horse;
		this.ogre = ogre;
		this.prostitution = prostitution;
		this.beast = beast;
		this.bird = bird;
		this.knot = knot;
	}
	
	protected int getAnalSex() { return analSex; }
	protected int getCreampies() { return creampies; }
	protected int getAnalEjaculations() { return analEjaculation; }
	protected int getOralSex() { return oralSex; }
	protected int getOralCreampies() { return oralCreampies; }
	protected int getFellatioEjaculations() { return fellatioEjaculation; }
	protected int getBellyful() { return bellyful; }
	protected int getHandy() { return handy; }
	protected boolean isCentaurSex() { return horse; }
	protected boolean isOgreSex() { return ogre; }
	protected boolean isProstitution() { return prostitution; }
	protected boolean isBeast() { return beast; }
	protected boolean isBird() { return bird; }
	protected boolean isKnot() { return knot; }
}
