package org.landroo.box2dtest;

// A simple test application how to use Box 2D with sensors

import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJoint;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Box2DTestActivity extends Activity implements Runnable, UIInterface
{
	private static final String TAG = "Box2DTestActivity";
	private static final float DEG2RAD = 0.0174532925199432957f;
	private static final float RAD2DEG = 57.295779513082320876f;

	// Box 2D
	static
	{
		System.loadLibrary("box2d");
	}
	private World mWorld;
	// private DistanceJoint distanceJoint;

	private int displayWidth;
	private int displayHeight;

	private Body[] bug = new Body[9];

	private boolean run = true;
	private boolean work = false;
	private boolean draw = false;
	private int sec = 0;

	private Thread thread;

	// mouse
	private UI ui = null;
	private float sX = 0;
	private float sY = 0;
	private float mX = 0;
	private float mY = 0;

	private float aX = 0;
	private float aY = 0;

	private float dX = 0;
	private float dY = 0;

	private long startTime = System.nanoTime();

	private long ballSize = 40;
	
	private String collObj = "";

	private Box2DView view;
	private class Box2DView extends View
	{
		private Vector2 vect;
		private float rot;
		private Iterator<Body> bodies;
		private Body body;
		private BodyData bodyData;
		private Drawable drawable;
		
		private Bitmap bmp;

		private final float rad = 180 / (float) Math.PI;

		private Paint paint = new Paint();

		public Box2DView(Context context)
		{
			super(context);

			paint.setTextSize(24);
			paint.setColor(Color.WHITE);
			
			bmp = createCorner(100, 100);
			drawable = new BitmapDrawable(bmp);
			drawable.setBounds(0, 0, 100, 100);
		}

		@Override
		protected synchronized void onDraw(Canvas canvas)
		{
			if (draw == true) return;
			draw = true;
			bodies = mWorld.getBodies();
			while (bodies.hasNext())
			{
				if ((body = bodies.next()) != null)
				{
					vect = body.getPosition();
					rot = body.getAngle() * rad;
					bodyData = (BodyData) body.getUserData();

					if (bodyData.drawable != null)
					{
						canvas.save();
						canvas.rotate(rot, vect.x, vect.y);
						canvas.translate(vect.x, vect.y);
						bodyData.drawable.draw(canvas);
						canvas.restore();
					}
					// canvas.drawText("" + rot, 0, 24, paint);
				}
			}

			// pieDrawable.draw(canvas);
			// ballDrawable.draw(canvas);
			canvas.drawText(collObj, 600, 100, paint);
			
			//drawable.draw(canvas);

			draw = false;

			super.onDraw(canvas);
		}

	}

	private BodyContact bodyContact = new BodyContact();

	private class BodyContact implements ContactListener
	{

		@Override
		public synchronized void beginContact(com.badlogic.gdx.physics.box2d.Contact contact)
		{
			// TODO Auto-generated method stub
			// Log.i(TAG, "beginContact");
			
			Body body;
			Iterator<Body> bodies;

			bodies = mWorld.getBodies();
			while (bodies.hasNext())
			{
				if((body = bodies.next()) != null)
				{
					if (bug[0] != null && body != null)
					{
						if (areBodiesContacted(bug[0], body, contact))
						{
							BodyData bd = (BodyData) body.getUserData();
							collObj = bd.name;
							//Log.i(TAG, "bug hit by a " + bd.name);
							break;
						}
					}
				}
			}

		}

		@Override
		public void endContact(com.badlogic.gdx.physics.box2d.Contact contact)
		{
			// TODO Auto-generated method stub
			// Log.i(TAG, "endContact");
		}

		@Override
		public void preSolve(com.badlogic.gdx.physics.box2d.Contact contact, Manifold oldManifold)
		{
			// TODO Auto-generated method stub
			// Log.i(TAG, "preSolve");
		}

		@Override
		public void postSolve(com.badlogic.gdx.physics.box2d.Contact contact, ContactImpulse impulse)
		{
			// TODO Auto-generated method stub
			// Log.i(TAG, "postSolve");
		}

		public boolean areBodiesContacted(Body body1, Body body2, Contact contact)
		{
			if (contact.getFixtureA().getBody().equals(body1) || contact.getFixtureB().getBody().equals(body1))
				if (contact.getFixtureA().getBody().equals(body2) || contact.getFixtureB().getBody().equals(body2))
					return true;

			return false;
		}
	}

	private class BodyData
	{
		public Drawable drawable;
		public String name;
		public int type;
		public float width;
		public float height;
		public float offX;
		public float offY;
	}

	private SensorManager sensorManager;
	private List<Sensor> sensorList = null;
	private float sx0 = 0, sy0 = 0, sz0 = 0;
	private float sx1 = 0, sy1 = 0, sz1 = 0;
	private float sx2 = 0, sy2 = 0, sz2 = 0;
	private float velocityX, velocityY;
	private SensorEventListener sensorEventListener = new SensorEventListener()
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
		}

		public void onSensorChanged(SensorEvent event)
		{
			if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
			{
				sx1 = event.values[0];// 0, 360 compass
				sy1 = event.values[1];// -180, 180 head-foot
				sz1 = event.values[2];// 180, -180 left-right

				if (sx0 == 0 && sy0 == 0 && sz0 == 0)
				{
					sx0 = sx1;
					sy0 = sy1;
					sz0 = sz1;

					sx2 = sx1;
					sy2 = sy1;
					sz2 = sz1;

					return;
				}

				velocityX -= sz2 - sz1;
				velocityY -= sy2 - sy1;

				sx2 = sx1;
				sy2 = sy1;
				sz2 = sz1;
				
				if(Math.abs(sz1) < 10)
				{
					Vector2 gravity = new Vector2(sz1 / -20, sy1 / -20 + 1);
					mWorld.setGravity(gravity);
				}
			}
			else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			{
			}
			else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_box2_dtest);

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensorList.size() > 0)
		{
			sensorManager.registerListener(sensorEventListener, (Sensor) sensorList.get(0), SensorManager.SENSOR_DELAY_NORMAL);
		}

		setStage();

		ui = new UI(this);

		view = new Box2DView(this);
		setContentView(view);

		thread = new Thread(this);
		thread.start();

		ballSize = displayWidth / 16;

		//bug[0] = addPie((displayWidth - displayWidth / 4) / 3, displayHeight / 4 * 3, displayWidth / 4, displayWidth / 8);
		// bug[1] = addPie((displayWidth - displayWidth / 4 ) / 3 * 2, displayHeight / 2, displayHeight / 4, displayHeight / 8);
		// bug = addBox((displayWidth - displayWidth / 4 ) /2, displayHeight / 2, displayWidth / 4, displayWidth / 8);
		bug[0] = addHalf(0, 1020, 100, 100, 90, BodyDef.BodyType.DynamicBody, mWorld);
		bug[1] = addQuarter(150, 1020, 100, 100);
		bug[2] = addCorner(280, 1020, 100, 100);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.box2_dtest, menu);
		return true;
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public synchronized void onResume()
	{
		super.onResume();
		run = true;
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public synchronized void onPause()
	{
		super.onPause();
		run = false;
		sensorManager.unregisterListener(sensorEventListener);
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return ui.tapEvent(event);
	}

	private void setStage()
	{
		// create gravity
		mWorld = new World(new Vector2(0, 1), true);
		mWorld.setContactListener(bodyContact);

		// add a fix body
		addWall(0, displayHeight - 50, displayWidth, 4);
		// addWall(0, 0, 4, displayHeight - 4);
		// addWall(displayWidth - 4, 0, 4, displayHeight - 4);

		// addWall((displayWidth / 2) - 1, displayHeight / 2, 2, displayHeight -
		// 4);
	}

	// add bug
	private Body addPie(float x, float y, float w, float h)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;

		PolygonShape polygonShape = new PolygonShape();
		Vector2[] vert = hPiePoly(w, h);

		polygonShape.set(vert);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.6f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.5f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = createHalfPie((int) w, (int) h);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "pie";

		Body body = mWorld.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}

	private Body addBox(float x, float y, float w, float h)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;

		Vector2[] vertices = { new Vector2(0, 0), new Vector2(w, 0), new Vector2(w, h), new Vector2(0, h) };

		PolygonShape polygonShape = new PolygonShape();
		polygonShape.set(vertices);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.6f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.5f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, w - 1, h - 1, paint);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "box";

		Body body = mWorld.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}

	private Body addWall(float x, float y, float w, float h)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.StaticBody;

		Vector2[] vertices = { new Vector2(0, 0), new Vector2(w, 0), new Vector2(w, h), new Vector2(0, h) };

		PolygonShape polygonShape = new PolygonShape();
		polygonShape.set(vertices);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.friction = 1f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, w - 1, h - 1, paint);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "wall";

		Body body = mWorld.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}

	private Body addBall(float x, float y, float r)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;

		CircleShape circle = new CircleShape();
		circle.setPosition(new Vector2(r, r));
		circle.setRadius(r);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circle;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.7f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.8f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = Bitmap.createBitmap((int) r * 2, (int) r * 2, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);

		canvas.drawCircle(r, r, r, paint);
		canvas.drawLine(r, r, r * 2, r, paint);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) r * 2, (int) r * 2);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "ball";

		Body body = mWorld.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}

	@Override
	public void run()
	{
		int msec = 0;
		while (run)
		{
			if (System.nanoTime() - startTime >= 1000000000) msec++;
			if (msec > 600 && work == false)
			{
				backThread();
				msec = 0;
			}
		}
	}

	private void backThread()
	{
		if (run)
		{
			int cnt = 0;
			work = true;
			Body body;
			Vector2 vect;
			Iterator<Body> bodies;

			mWorld.step(1f / 60f, 6, 2);
			mWorld.clearForces();
			view.postInvalidate();

			sec++;
			// add a new block by second
			if (sec == 1000)
			{
				sec = 0;

				// remove out of screen objects
				try
				{
					bodies = mWorld.getBodies();
					while (bodies.hasNext())
					{
						body = (Body) bodies.next();
						if (body != null)
						{
							vect = body.getPosition();

							if (vect.y > displayHeight * 2)
							{
								mWorld.destroyBody(body);
								break;
							}
						}
						cnt++;
					}
				}
				catch (Exception ex)
				{

				}

				if (cnt % 2 == 0) addBall((displayWidth - ballSize) / 2, ballSize, ballSize / 2);
				else addBox((displayWidth - 40) / 2, 0, 40, 80);
			}

			if (aX != 0 && aY != 0)
			{
				bug[0].applyForce(new Vector2(0, bug[0].getMass() * -700), bug[0].getWorldCenter());
				// addBall(aX, aY, 20);
				aX = 0;
				aY = 0;
			}

			if (Math.abs(dX) > 2)
			{
				bug[0].applyForce(new Vector2(bug[0].getMass() * 5 * dX, 0), bug[0].getWorldCenter());
				// vect = bug.getPosition();
				// bug.setTransform(new Vector2(vect.x + dX, vect.y),
				// bug.getAngle());
				dX = 0;
				dY = 0;
			}

			work = false;
		}

	}

	@Override
	public void onDown(float x, float y)
	{
		sX = x;
		sY = y;
	}

	@Override
	public void onUp(float x, float y)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onTap(float x, float y)
	{
		aX = x;
		aY = y;
	}

	@Override
	public void onHold(float x, float y)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onMove(float x, float y)
	{
		mX = x;
		mY = y;

		dX = mX - sX;
		dY = mY - sY;

		sX = mX;
		sY = mY;

		return;
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onDoubleTap(float x, float y)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub

	}

	private Bitmap createHalfPie(int w, int h)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		
		Vector2[] vect = hPiePoly(w, h);

		Path path = new Path();
		path.moveTo(vect[0].x , vect[0].y);

		for (int i = 1; i < 8; i++)
			path.lineTo(vect[i].x , vect[i].y);
		
		path.lineTo(0, h - 1);

		canvas.drawPath(path, paint);

		return bitmap;
	}

	private Vector2[] hPiePoly(float w, float h)
	{
		Vector2[] vect = new Vector2[8];
		double r = w / 2;
		double x, y, c = Math.PI;
		vect[0] = new Vector2(0, h);
		for (int i = 1; i < 8; i++)
		{
			c -= Math.PI / 7;
			x = r * Math.sin(c + Math.PI / 2);
			y = r * Math.cos(c + Math.PI / 2);
			vect[i] = new Vector2(w / 2 + (float) x, h + (float) y);
			//Log.i(TAG, "new Vector2(" + vect[i].x + ", " + vect[i].y + ")");
		}

		return vect;
	}
	
	// quarter
	private Bitmap createQuarterPie(int w, int h)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		
		Vector2[] vect = qPiePoly(w, h);

		Path path = new Path();
		path.moveTo(vect[0].x , vect[0].y);
		for (int i = 1; i < 8; i++)
			path.lineTo(vect[i].x , vect[i].y);
		
		path.lineTo(w - 1, 0);
		
		//canvas.drawRect(0, 0, w - 1, h - 1, paint);
		
		canvas.drawPath(path, paint);

		return bitmap;
	}

	private Vector2[] qPiePoly(float w, float h)
	{
		Vector2[] vect = new Vector2[8];// maximum size of polygon
		double r = w - 1, c = Math.PI / 2;
		float x, y;
		
		for (int i = 0; i < 7; i++)
		{
			x = (float)(r * Math.sin(c));
			y = (float)(r * Math.cos(c));
			vect[i] = new Vector2(x, y);
			c -= Math.PI / 12;
			
			Log.i(TAG, "" + i + " " + x + ", " + y);
		}
		vect[7] = new Vector2(0, 0);

		return vect;
	}

	private Body addQuarter(float x, float y, float w, float h)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;

		PolygonShape polygonShape = new PolygonShape();
		Vector2[] vert = qPiePoly(w, h);

		polygonShape.set(vert);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.6f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.5f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = createQuarterPie((int) w, (int) h);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "quarter";

		Body body = mWorld.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}
	
	private Body addCorner(float x, float y, float w, float h)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;
		
		Body body = mWorld.createBody(bodyDef);

		PolygonShape polygonShape = new PolygonShape();

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.6f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.5f;// Friction is used to make objects slide along each other realistically.
		
		Vector2[] vert;
		for(int j = 0; j < 6; j++)
		{
			vert = qPiePolyPart(w, h, j);
			polygonShape.set(vert);
			body.createFixture(fixtureDef);
		}

		Bitmap bitmap = createCorner((int) w, (int) h);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "corner";

		body.setUserData(bodyData);
		body.resetMassData();

		return body;
	}
	
	
	private Vector2[] qPiePolyPart(float w, float h, int part)
	{
		Vector2[] vect = new Vector2[3];
		double r = w - 1, c = Math.PI / 2;
		float x, y;
		
		c = (Math.PI / 12) * part;
		x = (float)(r * Math.sin(c));
		y = (float)(r * Math.cos(c));
		vect[0] = new Vector2(x, y);

		c = (Math.PI / 12) * (part + 1);
		x = (float)(r * Math.sin(c));
		y = (float)(r * Math.cos(c));
		vect[1] = new Vector2(x, y);
		
		vect[2] = new Vector2(w - 1, h - 1);

		return vect;
	}
	

	private Bitmap createCorner(int w, int h)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		
		Path path = new Path();
		
		Vector2[] vect;
		for(int j = 0; j < 6; j++)
		{
			vect = qPiePolyPart(w, h, j);
			path.moveTo(vect[0].x , vect[0].y);
			for (int i = 1; i < 3; i++)
				path.lineTo(vect[i].x , vect[i].y);
			path.lineTo(vect[0].x , vect[0].y);
		}		
		
		canvas.drawPath(path, paint);

		return bitmap;
	}
	
	// half polygon
	private Vector2[] halfPoly(float w, float h, float rot)
	{
		Vector2[] vect = new Vector2[8];
		double r = w / 2;
		double x, y, c = Math.PI - rot;
		for (int i = 0; i < 8; i++)
		{
			x = r * Math.sin(c + Math.PI / 2);
			y = r * Math.cos(c + Math.PI / 2);
			vect[i] = new Vector2(w / 2 + (float) x, h / 2 + (float) y);
			c -= Math.PI / 7;
		}

		return vect;
	}
	
	// half bitmap
	public Bitmap createHalf(int w, int h, float rot)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.WHITE);
		
		Vector2[] vect = halfPoly(w, h, rot);

		Path path = new Path();
		path.moveTo(vect[0].x , vect[0].y);

		for (int i = 1; i < 8; i++)
			path.lineTo(vect[i].x , vect[i].y);
		
		path.lineTo(vect[0].x , vect[0].y);

		canvas.drawPath(path, paint);

		return bitmap;
	}

	// add half body
	public Body addHalf(float x, float y, float w, float h, float rot, BodyDef.BodyType type, World world)
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.position.set(x, y);
		bodyDef.type = BodyDef.BodyType.DynamicBody;

		PolygonShape polygonShape = new PolygonShape();
		Vector2[] vert = halfPoly(w, h, rot * DEG2RAD);

		polygonShape.set(vert);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = polygonShape;
		fixtureDef.density = 1;// The fixture density is used to compute the mass properties of the parent body.
		fixtureDef.restitution = 0.6f;// Restitution is used to make objects bounce.
		fixtureDef.friction = 0.5f;// Friction is used to make objects slide along each other realistically.

		Bitmap bitmap = createHalf((int) w, (int) h, rot * DEG2RAD);
		Drawable drawable = new BitmapDrawable(bitmap);
		drawable.setBounds(0, 0, (int) w, (int) h);

		BodyData bodyData = new BodyData();
		bodyData.drawable = drawable;
		bodyData.name = "half";
		bodyData.type = 2;
		bodyData.width = w;
		bodyData.height = h;
		bodyData.offX = 0;
		bodyData.offY = 0;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);
		body.setUserData(bodyData);

		return body;
	}

}
