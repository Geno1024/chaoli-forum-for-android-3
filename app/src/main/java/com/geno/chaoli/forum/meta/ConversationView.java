package com.geno.chaoli.forum.meta;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.geno.chaoli.forum.PostActivity;
import com.geno.chaoli.forum.R;

public class ConversationView extends RelativeLayout
{
	private static final String TAG = "ConversationView";

	public Conversation conversation;

	public ConversationView(final Context context, final Conversation conversation)
	{
		this(context);
		View.inflate(context, R.layout.conversation_view, this);
		this.conversation = conversation;

		((TextView) findViewById(R.id.conversationId)).setText(String.format("%d", conversation.getConversationId()));
		((TextView) findViewById(R.id.title)).setText(conversation.getTitle());
		String excerpt = conversation.getExcerpt().split("\\n")[0];
		((TextView) findViewById(R.id.excerpt)).setText(excerpt.length() > 50 ?
				excerpt.substring(0, 50) + "…" : excerpt);
		((TextView) findViewById(R.id.replies)).setText(String.format("%d", conversation.getReplies()));
		((LinearLayout) findViewById(R.id.channel)).addView(new ChannelTextView(context, conversation.getChannel()));

		this.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO: 2016/2/12 0012 2123 Jump to post view.
				//Toast.makeText(context, ((ConversationView) v).conversation.getConversationId() + "", Toast.LENGTH_SHORT).show();
				Intent jmp = new Intent();
				jmp.putExtra("conversationId", ((ConversationView) v).conversation.getConversationId());
				jmp.setClass(context, PostActivity.class);
				context.startActivity(jmp);
			}
		});

		this.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				// TODO: 2016/2/12 0012 2131 MarkAsRead and Star.
				LinearLayout menuList = new LinearLayout(context);
				menuList.setPadding(Constants.paddingLeft, Constants.paddingTop,
						Constants.paddingRight, Constants.paddingBottom);

				AlertDialog.Builder menuBuilder = new AlertDialog.Builder(context).setView(menuList);
				final Dialog menu = menuBuilder.create();
				return true;
			}
		});
	}

	public ConversationView(Context context)
	{
		this(context, (AttributeSet)null);
	}

	public ConversationView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public ConversationView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}
}
