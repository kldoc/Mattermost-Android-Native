package com.kilogramm.mattermost.view.chat;

import android.content.Context;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.ChatListItemBinding;
import com.kilogramm.mattermost.databinding.LoadMoreLayoutBinding;
import com.kilogramm.mattermost.model.entity.post.Post;
import com.kilogramm.mattermost.tools.HrSpannable;
import com.kilogramm.mattermost.tools.MattermostTagHandler;
import com.kilogramm.mattermost.viewmodel.chat.ItemChatViewModel;
import com.vdurmont.emoji.EmojiParser;

import java.util.regex.Pattern;

/**
 * Created by Evgeny on 31.10.2016.
 */

public class PostViewHolder extends RecyclerView.ViewHolder {

    private ViewDataBinding mBinding;

    public static PostViewHolder createItem(LayoutInflater inflater, ViewGroup parent) {
        ChatListItemBinding binding = ChatListItemBinding
                .inflate(inflater, parent, false);
        return new PostViewHolder(binding);
    }

    public static PostViewHolder createLoadingTop(LayoutInflater inflater, ViewGroup parent) {
        LoadMoreLayoutBinding binding = LoadMoreLayoutBinding.inflate(inflater, parent, false);
        return new PostViewHolder(binding);
    }

    public static PostViewHolder createLoadingBottom(LayoutInflater inflater, ViewGroup parent) {
        LoadMoreLayoutBinding binding = LoadMoreLayoutBinding.inflate(inflater, parent, false);
        return new PostViewHolder(binding);
    }

    private PostViewHolder(ViewDataBinding binding) {
        super(binding.getRoot());
        mBinding = binding;
    }

    public void bindToItem(Post post, Context context, Boolean isTitle, Post root, OnItemClickListener listener) {
        if (post.getUpdateAt() != null && post.getUpdateAt() == Post.NO_UPDATE) {
            ((ChatListItemBinding) mBinding).sendStatusError.setOnClickListener(view -> {
                if (listener != null)
                    listener.OnItemClick(((ChatListItemBinding) mBinding).sendStatusError, post.getId());
            });
        }
        ((ChatListItemBinding) mBinding).controlMenu.setOnClickListener(view -> {
            if (listener != null) {
                listener.OnItemClick(((ChatListItemBinding) mBinding).controlMenu, post.getId());
            }
        });
        if (!post.isSystemMessage()) {
            ((ChatListItemBinding) mBinding).avatar.setOnClickListener(view -> {
                if (listener != null) {
                    listener.OnItemClick(((ChatListItemBinding) mBinding).avatar, post.getId());
                }
            });
        } else {
            ((ChatListItemBinding) mBinding).avatar.setOnClickListener(null);
        }
        ((ChatListItemBinding) mBinding).avatar.setTag(post);
        SpannableStringBuilder ssb = getSpannableStringBuilder(post, context);
        ((ChatListItemBinding) mBinding).message.setText(revertSpanned(ssb));
        ((ChatListItemBinding) mBinding).message.setMovementMethod(LinkMovementMethod.getInstance());

        if (((ChatListItemBinding) mBinding).getViewModel() == null) {
            ((ChatListItemBinding) mBinding).setViewModel(new ItemChatViewModel(post));
        } else {
            ((ChatListItemBinding) mBinding).getViewModel().setPost(post);

        }
        if (root != null)
            setRootMassage(root);
        else
            ((ChatListItemBinding) mBinding).linearLayoutRootPost.setVisibility(View.GONE);

        if (isTitle) {
            ((ChatListItemBinding) mBinding).getViewModel().setTitleVisibility(View.VISIBLE);
        } else {
            ((ChatListItemBinding) mBinding).getViewModel().setTitleVisibility(View.GONE);
        }
               /* AnimatedVectorDrawableCompat animatedVectorDrawableCompat
                        = AnimatedVectorDrawableCompat.create(context, R.drawable.vector_test_anim);
                mBinding.sendStatus.setImageDrawable(animatedVectorDrawableCompat);


            if(mBinding.sendStatus.getDrawable() instanceof Animatable){
                ((Animatable) mBinding.sendStatus.getDrawable()).start();
            }*/
        mBinding.executePendingBindings();
    }

    public void bindToLoadingTop() { }

    public void bindToLoadingBottom() { }

    private void setRootMassage(Post root) {
        if (root != null) {
            ((ChatListItemBinding) mBinding).filesViewRoot.setBackgroundColorComment();
            ((ChatListItemBinding) mBinding).filesViewRoot.setItems(root.getFilenames());
            ((ChatListItemBinding) mBinding).linearLayoutRootPost.setVisibility(View.VISIBLE);
            ((ChatListItemBinding) mBinding).nickRootPost.setText(root.getUser().getUsername());
            ((ChatListItemBinding) mBinding).getViewModel().loadImage(((ChatListItemBinding) mBinding).avatarRootPost, ((ChatListItemBinding) mBinding).getViewModel().getUrl(root));
            ((ChatListItemBinding) mBinding).messageRootPost.setText(revertSpanned(getSpannableStringBuilder(root, (mBinding).getRoot().getContext())).toString().trim());
        }
    }

    @NonNull
    private static SpannableStringBuilder getSpannableStringBuilder(Post post, Context context) {
        Spanned spanned;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(EmojiParser.parseToUnicode(post.getMessage()), Html.FROM_HTML_MODE_LEGACY, null, new MattermostTagHandler());
        } else {
            spanned = Html.fromHtml(EmojiParser.parseToUnicode(post.getMessage()), null, new MattermostTagHandler());
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(spanned);
        Linkify.addLinks(ssb, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

        Linkify.addLinks(ssb, Pattern.compile("\\B@([\\w|.]+)\\b"), null, (s, start, end) -> {
            ssb.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.colorPrimary)),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return true;
        }, null);

        Linkify.addLinks(ssb, Pattern.compile("<hr>.*<\\/hr>"), null, (charSequence, i, i1) -> {
            String s = charSequence.toString();
            StringBuilder builder = new StringBuilder();
            for (int k = i; k < i1; k++) {
                builder.append(' ');
            }
            ssb.replace(i, i1, builder.toString());
            ssb.setSpan(new HrSpannable(context.getResources().getColor(R.color.light_grey)), i, i1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return true;
        }, null);
        return ssb;
    }

    static Spannable revertSpanned(Spanned stext) {
        Object[] spans = stext.getSpans(0, stext.length(), Object.class);
        Spannable ret = Spannable.Factory.getInstance().newSpannable(stext.toString());
        if (spans != null && spans.length > 0) {
            for (int i = spans.length - 1; i >= 0; --i) {
                ret.setSpan(spans[i], stext.getSpanStart(spans[i]), stext.getSpanEnd(spans[i]), stext.getSpanFlags(spans[i]));
            }
        }

        return ret;
    }
}